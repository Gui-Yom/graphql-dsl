package marais.graphql.dsl

import kotlin.reflect.KFunction

/**
 * Set of functions that calls the lambda passed as argument.
 * We can call these functions through reflections since they are concrete ones.
 * They are all static in the sense of the JVM, so we do not need to pass a receiver.
 */
@PublishedApi
internal object Lambdas {

    @PublishedApi
    internal fun indirectCallSuspend(arity: Int): KFunction<*> {
        return when (arity) {
            0 -> Lambdas::indirectCallSuspend0
            1 -> Lambdas::indirectCallSuspend1
            2 -> Lambdas::indirectCallSuspend2
            3 -> Lambdas::indirectCallSuspend3
            4 -> Lambdas::indirectCallSuspend4
            5 -> Lambdas::indirectCallSuspend5
            6 -> Lambdas::indirectCallSuspend6
            else -> throw Exception("Unsupported arity $arity")
        }
    }

    @JvmStatic
    internal suspend fun indirectCallSuspend0(rec: Any, fetcher: suspend Any.() -> Any?): Any? {
        return fetcher(rec)
    }

    @JvmStatic
    internal suspend fun indirectCallSuspend1(rec: Any, fetcher: suspend Any.(Any?) -> Any?, arg0: Any?): Any? {
        return fetcher(rec, arg0)
    }

    @JvmStatic
    internal suspend fun indirectCallSuspend2(
        rec: Any,
        fetcher: suspend Any.(Any?, Any?) -> Any?,
        arg0: Any?,
        arg1: Any?
    ): Any? {
        return fetcher(rec, arg0, arg1)
    }

    @JvmStatic
    internal suspend fun indirectCallSuspend3(
        rec: Any,
        fetcher: suspend Any.(Any?, Any?, Any?) -> Any?,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?
    ): Any? {
        return fetcher(rec, arg0, arg1, arg2)
    }

    @JvmStatic
    internal suspend fun indirectCallSuspend4(
        rec: Any,
        fetcher: suspend Any.(Any?, Any?, Any?, Any?) -> Any?,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?
    ): Any? {
        return fetcher(rec, arg0, arg1, arg2, arg3)
    }

    @JvmStatic
    internal suspend fun indirectCallSuspend5(
        rec: Any,
        fetcher: suspend Any.(Any?, Any?, Any?, Any?, Any?) -> Any?,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?,
        arg4: Any?
    ): Any? {
        return fetcher(rec, arg0, arg1, arg2, arg3, arg4)
    }

    @JvmStatic
    internal suspend fun indirectCallSuspend6(
        rec: Any,
        fetcher: suspend Any.(Any?, Any?, Any?, Any?, Any?, Any?) -> Any?,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?,
        arg4: Any?,
        arg5: Any?
    ): Any? {
        return fetcher(rec, arg0, arg1, arg2, arg3, arg4, arg5)
    }
}
