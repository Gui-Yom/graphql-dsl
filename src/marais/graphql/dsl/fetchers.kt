package marais.graphql.dsl

import graphql.TrivialDataFetcher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.future
import kotlinx.coroutines.reactive.asPublisher
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.callSuspend


// TODO Since we know types at compile time, mapping could be done statically instead of inside the DataFetcher

fun transformMap(value: Any?): Any? {
    return when (value) {
        is Map<*, *> -> value.entries
        is CompletableFuture<*> -> value.thenApply { (it as Map<*, *>).entries }
        else -> value
    }
}

/**
 * The graphql engine accepts simple values, CompletableFuture and Publisher, we map any types to those
 */
fun transformAsyncResult(value: Any?, context: CoroutineContext = EmptyCoroutineContext): Any? {
    return when (value) {
        is Deferred<*> -> value.asCompletableFuture()
        is Flow<*> -> (value as Flow<Any>).asPublisher(context)
        else -> value
    }
}

/**
 * @param receiver should not be null for root queries
 */
fun <R, O> propertyFetcher(property: KProperty1<R, O>, receiver: R? = null): DataFetcher<Any?> {
    return receiver?.let { TrivialDataFetcher { property.get(receiver) } }
        ?: TrivialDataFetcher { property.get(it.getSource()) }
}

/**
 * Create the appropriate [DataFetcher] for a function.
 * Handles the case where the function is suspend or returns Flow
 */
fun functionFetcher(
    func: KFunction<Any?>,
    args: List<Argument>,
    scope: CoroutineScope = GlobalScope,
    context: CoroutineContext = EmptyCoroutineContext,
    receiver: Any? = null
): DataFetcher<Any?> {
    return if (func.isSuspend) {
        suspendFetcher(scope, context) { env ->
            func.callSuspend(
                receiver ?: env.getSource(),
                *args.map {
                    it.resolve<Any>(env)
                }.toTypedArray()
            )
        }
    } else {
        DataFetcher { env ->
            transformAsyncResult(
                func.call(
                    receiver ?: env.getSource(),
                    *args.map {
                        it.resolve<Any>(env)
                    }.toTypedArray()
                ),
                context
            )
        }
    }
}

/**
 * Convert suspend call to a java CompletableFuture
 */
fun <O> suspendFetcher(
    scope: CoroutineScope = GlobalScope,
    context: CoroutineContext = EmptyCoroutineContext,
    inner: suspend (DataFetchingEnvironment) -> O
): DataFetcher<Any?> {
    return DataFetcher {
        scope.future(context) {
            transformAsyncResult(inner(it), context)
        }
    }
}
