package marais.graphql.dsl

import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.typeOf

internal fun KParameter.createArgument(context: SchemaBuilderContext): Argument {
    return createArgument(
        name ?: throw Exception("Parameter $this must have a name (no _ allowed)"),
        type,
        context.takeArgDesc(name!!) ?: extractDesc(),
        context
    )
}

internal fun createArgument(name: String, type: KType, desc: String?, context: SchemaBuilderContext): Argument {
    return when (type.kclass) {
        DataFetchingEnvironment::class -> EnvArgument(name)
        GraphQLContext::class -> ContextArgument(name)
        List::class -> ListArgument(name, type, desc, context)
        in context.idCoercers -> IdArgument(name, type, desc, context.idCoercers[type.kclass]!!)
        else -> when {
            type.kclass.isEnum() -> EnumArgument(name, type, desc)
            context.isInputType(type.kclass) -> InputObjectArgument(name, type, desc, context)
            else -> NormalArgument(name, type, desc)
        }
    }
}

sealed class Argument(val name: String, val type: KType, val description: String?) {

    internal val isShownInSchema: Boolean
        get() = this !is EnvArgument && this !is ContextArgument && this !is StaticArgument

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

/**
 * For the ID scalar
 */
private class IdArgument(name: String, type: KType, desc: String?, private val idCoercer: IdCoercer<*>) :
    Argument(name, type, desc) {

    override fun resolve(input: Any?): Any? {
        return idCoercer.invoke(input as? String?)
    }
}

class StaticArgument(private val value: Any?) : Argument("static", typeOf<Any>(), null) {
    override fun resolve(env: DataFetchingEnvironment): Any? {
        return value
    }

    override fun resolve(input: Any?): Any? {
        return value
    }
}

/**
 * For injecting DataFetchingEnvironment instance
 */
private class EnvArgument(name: String) : Argument(name, typeOf<DataFetchingEnvironment>(), null) {

    override fun resolve(env: DataFetchingEnvironment): DataFetchingEnvironment = env

    override fun resolve(input: Any?): Any = throw UnsupportedOperationException("No nested env")
}

/**
 * For injecting GraphQLContext instance
 */
private class ContextArgument(name: String) : Argument(name, typeOf<GraphQLContext>(), null) {

    override fun resolve(env: DataFetchingEnvironment): GraphQLContext = env.graphQlContext

    override fun resolve(input: Any?): Any = throw UnsupportedOperationException("No nested env")
}

/**
 * For input objects
 */
private class InputObjectArgument(name: String, type: KType, desc: String?, context: SchemaBuilderContext) :
    Argument(name, type, desc) {

    private val constructor = context.getInputType(type.kclass)!!.constructor
    private val constructorArguments = context.getInputType(type.kclass)!!.fields.map { (fname, ftype) ->
        if (ftype.classifier == type.classifier)
        // Check for nullable self reference
        // We return 'this' to prevent infinite recursion by calling another constructor for the same type
        // The second item of the pair overwrites the argument name in resolve if non-null
            if (ftype.isMarkedNullable) this to fname
            else throw Exception("Non null self reference in input object: $ftype")
        else createArgument(fname, ftype, null, context) to null
    }

    override fun resolve(input: Any?): Any? {
        if (input == null) return null
        if (input !is Map<*, *>) throw Exception("Input object requires a Map as argument")

        return constructor.call(*constructorArguments.map { (arg, nestedName) ->
            arg.resolve(input[nestedName ?: arg.name])
        }.toTypedArray())
    }
}

/**
 * For scalars
 */
private class NormalArgument(name: String, type: KType, desc: String?) : Argument(name, type, desc) {

    override fun resolve(input: Any?): Any? = input
}

/**
 * For enums
 */
private class EnumArgument(name: String, type: KType, desc: String?) : Argument(name, type, desc) {

    private val constants = type.kclass.java.enumConstants as Array<Enum<*>>

    override fun resolve(input: Any?): Any? = constants.find { it.name == input }
}

/**
 * For lists
 */
private class ListArgument(name: String, type: KType, desc: String?, context: SchemaBuilderContext) :
    Argument(name, type, desc) {

    val innerArg = createArgument("", type.unwrap(), null, context)

    override fun resolve(input: Any?): Any? {
        if (input == null) return null
        if (input !is List<*>) throw Exception("List input requires List as input")

        return input.map { innerArg.resolve(it) }
    }
}
