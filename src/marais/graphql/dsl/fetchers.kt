package marais.graphql.dsl

import graphql.execution.*
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.reactive.asPublisher
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspend

internal fun List<Argument>.resolve(env: DataFetchingEnvironment): Array<Any?> =
    if (isEmpty()) emptyArray() else map { it.resolve(env) }.toTypedArray()

// We don't extract the function return type directly from the kfunction instance since they may be indirect calls
// We let the caller make the appropriate reflection calls

fun KFunction<*>.fetcher(
    returnType: KType,
    args: List<Argument>,
    receiver: Any? = null,
    schemaCtx: SchemaBuilderContext,
    scope: CoroutineScope = GlobalScope,
    corCtx: CoroutineContext = EmptyCoroutineContext,
): DataFetcher<Any?> {
    return if (this.isSuspend) when (returnType.classifier) {
        Map::class -> DataFetcher { env ->
            val args = args.resolve(env)
            scope.future(corCtx) {
                (this@fetcher as KFunction<Map<String, Any>>).callSuspend(
                    env.getSource() ?: receiver,
                    *args
                ).entries
            }
        }

        Deferred::class -> if (returnType.unwrap().classifier == Map::class)
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(corCtx) {
                    (this@fetcher as KFunction<Deferred<Map<Any, Any>>>).callSuspend(
                        env.getSource() ?: receiver,
                        *args
                    ).await().entries
                }
            }
        else
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(corCtx) {
                    (this@fetcher as KFunction<Deferred<Any>>).callSuspend(env.getSource() ?: receiver, *args)
                        .await()
                }
            }

        Flow::class -> if (schemaCtx.convertFlowToPublisher)
            if (returnType.unwrap().classifier == Map::class)
                DataFetcher { env ->
                    val args = args.resolve(env)
                    scope.future(corCtx) {
                        (this@fetcher as KFunction<Flow<Map<Any, Any?>>>).callSuspend(
                            env.getSource() ?: receiver,
                            *args
                        ).map { it.entries }.asPublisher(coroutineContext)
                    }
                }
            else
                DataFetcher { env ->
                    val args = args.resolve(env)
                    scope.future(corCtx) {
                        (this@fetcher as KFunction<Flow<Any>>).callSuspend(env.getSource() ?: receiver, *args)
                            .asPublisher(coroutineContext)
                    }
                }
        else
            if (returnType.unwrap().classifier == Map::class)
                DataFetcher { env ->
                    val args = args.resolve(env)
                    scope.future(corCtx) {
                        (this@fetcher as KFunction<Flow<Any>>).callSuspend(env.getSource() ?: receiver, *args)
                            .map { (it as Map<Any, Any?>).entries }
                    }
                }
            else defaultFetcherSuspend(this, receiver, args, scope, corCtx)

        // CompletableFuture is a special value, we can't return CompletableFuture<CompletableFuture>
        // Since the behaviour is to convert the suspend function to a future, we need to unwrap it.
        CompletableFuture::class -> if (returnType.unwrap().classifier == Map::class)
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(corCtx) {
                    (this@fetcher as KFunction<CompletableFuture<Map<Any, Any?>>>).callSuspend(
                        env.getSource() ?: receiver, *args
                    ).await().entries
                }
            }
        else
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(corCtx) {
                    // is there a better way of doing this ?
                    (this@fetcher as KFunction<CompletableFuture<Any?>>).callSuspend(
                        env.getSource() ?: receiver, *args
                    ).await()
                }
            }

        else -> defaultFetcherSuspend(this, receiver, args, scope, corCtx)

    } else when (returnType.classifier) {
        Map::class ->
            DataFetcher { env ->
                val args = args.resolve(env)
                (this as KFunction<Map<String, Any>>).call(env.getSource() ?: receiver, *args).entries
            }

        Deferred::class -> if (returnType.unwrap().classifier == Map::class)
            DataFetcher { env ->
                val args = args.resolve(env)
                (this as KFunction<Deferred<Map<Any, Any>>>).call(env.getSource() ?: receiver, *args)
                    .asCompletableFuture()
                    .thenApply(Map<*, *>::entries)
            }
        else
            DataFetcher { env ->
                val args = args.resolve(env)
                (this as KFunction<Deferred<Any>>).call(env.getSource() ?: receiver, *args).asCompletableFuture()
            }

        Flow::class -> if (schemaCtx.convertFlowToPublisher)
            if (returnType.unwrap().classifier == Map::class)
                DataFetcher { env ->
                    val args = args.resolve(env)
                    (this@fetcher as KFunction<Flow<Map<Any, Any?>>>).call(env.getSource() ?: receiver, *args)
                        .map { it.entries }
                        .asPublisher(corCtx)
                }
            else
                DataFetcher { env ->
                    val args = args.resolve(env)
                    (this@fetcher as KFunction<Flow<Any>>).call(env.getSource() ?: receiver, *args)
                        .asPublisher(corCtx)
                }
        else
            if (returnType.unwrap().classifier == Map::class)
                DataFetcher { env ->
                    val args = args.resolve(env)
                    (this@fetcher as KFunction<Flow<Any>>).call(env.getSource() ?: receiver, *args)
                        .map { (it as Map<Any, Any?>).entries }
                }
            else defaultFetcher(this, receiver, args)

        CompletableFuture::class -> if (returnType.unwrap().classifier == Map::class)
            DataFetcher { env ->
                val args = args.resolve(env)
                (this@fetcher as KFunction<CompletableFuture<Map<Any, Any?>>>).call(env.getSource() ?: receiver, *args)
                    .thenApply { it.entries }
            }
        else defaultFetcher(this, receiver, args)

        else -> defaultFetcher(this, receiver, args)
    }
}

private fun defaultFetcher(func: KFunction<*>, receiver: Any?, args: List<Argument>): DataFetcher<Any?> =
    DataFetcher { env ->
        val args = args.resolve(env)
        func.call(env.getSource() ?: receiver, *args)
    }

private fun defaultFetcherSuspend(
    func: KFunction<*>,
    receiver: Any?,
    args: List<Argument>,
    scope: CoroutineScope,
    context: CoroutineContext
): DataFetcher<Any?> = DataFetcher { env ->
    val args = args.resolve(env)
    scope.future(context) {
        func.callSuspend(env.getSource() ?: receiver, *args)
    }
}
