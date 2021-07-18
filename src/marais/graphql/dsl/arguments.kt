package marais.graphql.dsl

import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

internal fun KParameter.createArgument(context: SchemaBuilderContext): Argument {
    return when (type.kclass) {
        DataFetchingEnvironment::class -> EnvArgument(this)
        in context.idCoercers -> IdArgument(this, context.idCoercers[type.kclass]!!)
        else -> if (context.isInputType(type.kclass)) InputObjectArgument(this, context) else NormalArgument(this)
    }
}

sealed class Argument(val name: String, val type: KType) {

    /**
     * Resolve this argument directly from the environment.
     * By default, this calls the other resolve with the environment arguments.
     */
    open fun resolve(env: DataFetchingEnvironment): Any? {
        return resolve(env.arguments[name])
    }

    /**
     * Resolves this argument from a map of arguments.
     */
    abstract fun resolve(input: Any?): Any?
}

class IdArgument(name: String, type: KType, private val idCoercer: IdCoercer<*>) : Argument(name, type) {

    constructor(param: KParameter, idCoercer: IdCoercer<*>) : this(
        param.name ?: "anon",
        param.type,
        idCoercer
    )

    override fun resolve(input: Any?): Any? {
        return idCoercer.invoke(input as? String?)
    }
}

class EnvArgument(name: String) : Argument(name, typeOf<DataFetchingEnvironment>()) {

    constructor(param: KParameter) : this(
        param.name ?: "anon"
    )

    override fun resolve(env: DataFetchingEnvironment): DataFetchingEnvironment = env

    override fun resolve(input: Any?): Any = throw UnsupportedOperationException("No nested env")
}

class InputObjectArgument(name: String, type: KType, context: SchemaBuilderContext) : Argument(name, type) {

    constructor(param: KParameter, context: SchemaBuilderContext) : this(
        param.name ?: "anon",
        param.type,
        context
    )

    private val constructor = type.kclass.primaryConstructor!!
    private val constructorArguments = constructor.parameters.map {
        // TODO verify nullability of self reference
        if (it.type.classifier == type.classifier)
            if (it.type.isMarkedNullable) this
            else throw Exception("Non null self reference in input object: ${it.type}")
        else it.createArgument(context)
    }

    override fun resolve(input: Any?): Any? {
        if (input == null) return null
        if (input !is Map<*, *>) throw Exception("Input object requires a Map as argument")

        return constructor.call(*constructorArguments.map { arg ->
            arg.resolve(input[arg.name])
        }.toTypedArray())
    }
}

class NormalArgument(name: String, type: KType) : Argument(name, type) {

    constructor(param: KParameter) : this(
        param.name ?: "anon",
        param.type,
    )

    override fun resolve(input: Any?): Any? = input
}
