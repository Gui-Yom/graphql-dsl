package marais.graphql.dsl

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import marais.graphql.generator.functionFetcher
import marais.graphql.generator.propertyFetcher
import marais.graphql.generator.representationType
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.valueParameters

sealed class Field(val name: String, val description: String? = null) {

    abstract val arguments: List<Argument>
    abstract val dataFetcher: DataFetcher<Any?>
    abstract val outputType: KType
}

data class Argument(val name: String, val type: KType) {

    constructor(param: KParameter) : this(param.name!!, param.type)

    companion object {
        val envType = DataFetchingEnvironment::class.createType()
    }

    fun <T> resolve(env: DataFetchingEnvironment): T {
        return if (type == envType) {
            env as T
        } else {
            env.getArgument(name)
        }
    }

    fun isSpecialType() = type == envType
}

class CustomField(
        name: String,
        description: String? = null,
        override val outputType: KType,
        override val arguments: List<Argument> = emptyList(),
        override val dataFetcher: DataFetcher<Any?>
) : Field(name, description)

class PropertyField<R>(
        val property: KProperty1<R, Any?>,
        name: String? = null,
        description: String? = null,
        instance: R? = null
) : Field(name ?: property.name, description) {

    override val dataFetcher: DataFetcher<Any?> = propertyFetcher(property, instance)
    override val outputType: KType = property.returnType.representationType()
    override val arguments: List<Argument> = emptyList()
}

// FIXME it is currently impossible to specify the receiver for the KFunction
class FunctionField<R>(
        val func: KFunction<Any?>,
        name: String? = null,
        description: String? = null,
        instance: R? = null
) : Field(name ?: func.name, description) {

    override val outputType: KType = func.returnType.representationType()
    override val arguments: MutableList<Argument> = mutableListOf()

    // Might include special types that should not appear on the schema
    val funcArgs = mutableListOf<Argument>()

    init {
        for (it in func.valueParameters) {
            val arg = Argument(it)
            funcArgs += arg
            if (!arg.isSpecialType())
                arguments += arg
        }
    }

    override val dataFetcher: DataFetcher<Any?> = functionFetcher(func, funcArgs, receiver = instance)
}
