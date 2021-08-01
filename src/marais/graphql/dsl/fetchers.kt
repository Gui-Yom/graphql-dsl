package marais.graphql.dsl

import graphql.execution.*
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.future
import kotlinx.coroutines.reactive.asPublisher
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
    scope: CoroutineScope = GlobalScope,
    context: CoroutineContext = EmptyCoroutineContext,
): DataFetcher<Any> {
    return if (this.isSuspend) when (returnType.classifier) {
        Map::class -> DataFetcher { env ->
            val args = args.resolve(env)
            scope.future(context) {
                (this@fetcher as KFunction<Map<String, Any>>).callSuspend(
                    env.getSource() ?: receiver,
                    *args
                ).entries
            }
        }

        Deferred::class -> if (returnType.unwrap().classifier == Map::class)
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(context) {
                    (this@fetcher as KFunction<Deferred<Map<Any, Any>>>).callSuspend(
                        env.getSource() ?: receiver,
                        *args
                    ).await().entries
                }
            }
        else
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(context) {
                    (this@fetcher as KFunction<Deferred<Any>>).callSuspend(env.getSource() ?: receiver, *args)
                        .await()
                }
            }

        Flow::class -> if (returnType.unwrap().classifier == Map::class)
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(context) {
                    (this@fetcher as KFunction<Flow<Any>>).callSuspend(env.getSource() ?: receiver, *args)
                        .asPublisher(context)
                }
            }
        else
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(context) {
                    (this@fetcher as KFunction<Flow<Any>>).callSuspend(env.getSource() ?: receiver, *args)
                        .asPublisher(context)
                }
            }

        else ->
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(context) {
                    this@fetcher.callSuspend(env.getSource() ?: receiver, *args)
                }
            }

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

        Flow::class -> if (returnType.unwrap().classifier == Map::class)
            DataFetcher { env ->
                val args = args.resolve(env)
                (this@fetcher as KFunction<Flow<Any>>).call(env.getSource() ?: receiver, *args)
                    .asPublisher(context)
            }
        else
            DataFetcher { env ->
                val args = args.resolve(env)
                (this@fetcher as KFunction<Flow<Any>>).call(env.getSource() ?: receiver, *args)
                    .asPublisher(context)
            }

        else ->
            DataFetcher { env ->
                val args = args.resolve(env)
                this.call(env.getSource() ?: receiver, *args)
            }
    }
}
