package marais.graphql.dsl

import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal fun KParameter.createArgument(idCoercers: Map<KClass<*>, IdCoercer<*>>): Argument {
    return when (type.kclass) {
        DataFetchingEnvironment::class -> EnvArgument(this)
        in idCoercers -> IdArgument(this, idCoercers[type.kclass]!!)
        else -> NormalArgument(this)
    }
}

sealed class Argument(val name: String, val type: KType) {

    abstract fun <T> resolve(env: DataFetchingEnvironment): T
}

class IdArgument(name: String, type: KType, private val idCoercer: IdCoercer<*>) : Argument(name, type) {

    constructor(param: KParameter, idCoercer: IdCoercer<*>) : this(
        param.name ?: "anon",
        param.type,
        idCoercer
    )

    override fun <T> resolve(env: DataFetchingEnvironment): T = idCoercer.invoke(env.getArgument(name)) as T
}

class EnvArgument(name: String) : Argument(name, typeOf<DataFetchingEnvironment>()) {

    constructor(param: KParameter) : this(
        param.name ?: "anon"
    )

    override fun <T> resolve(env: DataFetchingEnvironment): T = env as T
}

class NormalArgument(name: String, type: KType) : Argument(name, type) {

    constructor(param: KParameter) : this(
        param.name ?: "anon",
        param.type,
    )

    override fun <T> resolve(env: DataFetchingEnvironment): T = env.getArgument(name)
}
