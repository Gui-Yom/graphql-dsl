package marais.graphql.dsl

import graphql.execution.*
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.future
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspend

fun List<Argument>.resolve(env: DataFetchingEnvironment): List<Any?> =
    if (isEmpty()) this else map { it.resolve(env) }

// We don't extract the function return type directly from the kfunction instance since they may be indirect calls
// We let the caller make the appropriate reflection calls

fun KFunction<*>.fetcher(
    returnType: KType,
    args: List<Argument>,
    scope: CoroutineScope = GlobalScope,
    context: CoroutineContext = EmptyCoroutineContext,
): DataFetcher<Any> {
    return if (this.isSuspend) when (returnType.classifier) {
        Map::class -> {
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(context) {
                    (this@fetcher as KFunction<Map<String, Any>>).callSuspend(
                        env.getSource(),
                        *args.toTypedArray()
                    ).entries
                }
            }
        }
        Deferred::class -> {
            if (returnType.unwrap().classifier == Map::class) {
                DataFetcher { env ->
                    val args = args.resolve(env)
                    scope.future(context) {
                        (this@fetcher as KFunction<Deferred<Map<Any, Any>>>).callSuspend(
                            env.getSource(),
                            *args.toTypedArray()
                        ).await().entries
                    }
                }
            } else {
                DataFetcher { env ->
                    val args = args.resolve(env)
                    scope.future(context) {
                        (this@fetcher as KFunction<Deferred<Any>>).callSuspend(env.getSource(), *args.toTypedArray())
                            .await()
                    }
                }
            }
        }
        else -> {
            DataFetcher { env ->
                val args = args.resolve(env)
                scope.future(context) {
                    this@fetcher.callSuspend(env.getSource(), *args.toTypedArray())
                }
            }
        }
    } else when (returnType.classifier) {
        Map::class -> {
            DataFetcher { env ->
                val args = args.resolve(env)
                (this as KFunction<Map<String, Any>>).call(env.getSource(), *args.toTypedArray()).entries
            }
        }
        Deferred::class -> {
            if (returnType.unwrap().classifier == Map::class) {
                DataFetcher { env ->
                    val args = args.resolve(env)
                    (this as KFunction<Deferred<Map<Any, Any>>>).call(env.getSource(), *args.toTypedArray())
                        .asCompletableFuture()
                        .thenApply(Map<*, *>::entries)
                }
            } else {
                DataFetcher { env ->
                    val args = args.resolve(env)
                    (this as KFunction<Deferred<Any>>).call(env.getSource(), *args.toTypedArray()).asCompletableFuture()
                }
            }
        }
        else -> {
            DataFetcher { env ->
                val args = args.resolve(env)
                this.call(env.getSource(), *args.toTypedArray())
            }
        }
    }
}
