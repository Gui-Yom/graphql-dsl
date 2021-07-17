package marais.graphql.dsl

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.valueParameters

internal fun checkType(type: KType) {
    if (type.hasStarProjection()) {
        throw Exception("Type can't have star projected type arguments")
    }
}

data class Argument(val name: String, val type: KType, val inputCoercer: IdConverter<*>?) {

    constructor(param: KParameter, inputCoercers: Map<KClass<*>, IdConverter<*>>) : this(
        param.name ?: "anon",
        param.type,
        inputCoercers[param.type.classifier as KClass<*>]
    )

    init {
        checkType(type)
    }

    companion object {
        val envType = DataFetchingEnvironment::class.createType()
    }

    fun <T> resolve(env: DataFetchingEnvironment): T {
        return if (type == envType) {
            env as T
        } else {
            if (inputCoercer != null) {
                inputCoercer.invoke(env.getArgument(name)) as T
            } else {
                env.getArgument(name)
            }
        }
    }

    /**
     * @return true if this argument is of a type that should not be exposed on the schema
     */
    fun isSpecialType() = type == envType
}

sealed class Field(val name: String, val description: String? = null) {

    /**
     * Field arguments as displayed in the schema, no special types
     */
    abstract val arguments: List<Argument>

    /**
     * The code behind this field returning a value
     */
    abstract val dataFetcher: DataFetcher<Any?>
    abstract val outputType: KType
}

class CustomField(
    name: String,
    description: String? = null,
    override val outputType: KType,
    override val arguments: List<Argument> = emptyList(),
    override val dataFetcher: DataFetcher<Any?>
) : Field(name, description) {

    init {
        checkType(outputType)
    }
}

class PropertyField<R>(
    property: KProperty1<R, Any?>,
    name: String,
    description: String? = null,
    instance: R? = null
) : Field(name, description) {

    override val dataFetcher: DataFetcher<Any?> = propertyFetcher(property, instance)
    override val outputType: KType = property.returnType.unwrapAsyncType()
    override val arguments: List<Argument> = emptyList()

    init {
        checkType(outputType)
    }
}

// FIXME it is currently impossible to specify the receiver for the KFunction
class FunctionField<R>(
    func: KFunction<Any?>,
    name: String,
    description: String? = null,
    instance: R? = null,
    inputCoercers: Map<KClass<*>, IdConverter<*>>
) : Field(name, description) {

    override val outputType: KType = func.returnType.unwrapAsyncType()

    init {
        checkType(outputType)
    }

    override val arguments: MutableList<Argument> = mutableListOf()

    // Might include special types that should not appear on the schema
    val funcArgs = mutableListOf<Argument>()

    init {
        for (it in func.valueParameters) {
            val arg = Argument(it, inputCoercers)
            funcArgs += arg
            if (!arg.isSpecialType())
                arguments += arg
        }
    }

    override val dataFetcher: DataFetcher<Any?> = functionFetcher(func, funcArgs, receiver = instance)
}
