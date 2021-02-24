package marais.graphql.dsl

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.PropertyDataFetcher
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.valueParameters

sealed class Field<O>(val name: String, val description: String? = null) {

    abstract val arguments: List<Argument>
    abstract val dataFetcher: DataFetcher<O>
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

class CustomField<O>(
    name: String,
    description: String? = null,
    override val outputType: KType,
    override val arguments: List<Argument> = emptyList(),
    override val dataFetcher: DataFetcher<O>
) : Field<O>(name, description)

class PropertyField<R, O>(val property: KProperty1<R, O>, name: String? = null, description: String? = null) :
    Field<O>(name ?: property.name, description) {

    override val dataFetcher: DataFetcher<O> = PropertyDataFetcher.fetching(property.getter)
    override val outputType: KType = property.returnType
    override val arguments: List<Argument> = emptyList()
}

// FIXME is it currently impossible to specify the receiver for the KFunction ?
class FunctionField<R, O>(val func: KFunction<O>, name: String? = null, description: String? = null) :
    Field<O>(name ?: func.name, description) {

    override val outputType: KType = func.returnType
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

    override val dataFetcher: DataFetcher<O> = DataFetcher { env ->
        func.call(
            env.getSource(),
            *funcArgs.map {
                it.resolve<Any>(env)
            }.toTypedArray()
        )
    }
}