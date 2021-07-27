package marais.graphql.dsl

import graphql.schema.DataFetcher
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.valueParameters

sealed class Field(val name: String, val description: String?) {

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
    description: String?,
    override val outputType: KType,
    override val arguments: List<Argument> = emptyList(),
    override val dataFetcher: DataFetcher<Any?>
) : Field(name, description)

internal class PropertyField<R>(
    property: KProperty1<R, Any?>,
    name: String,
    description: String?,
    instance: R? = null
) : Field(name, description ?: property.extractDesc()) {

    override val dataFetcher: DataFetcher<Any?> = propertyFetcher(property, instance)
    override val outputType: KType = property.returnType.unwrapAsyncType()
    override val arguments: List<Argument> = emptyList()
}

internal class FunctionField<R>(
    func: KFunction<Any?>,
    name: String,
    description: String?,
    instance: R?,
    context: SchemaBuilderContext
) : Field(name, description ?: func.extractDesc()) {

    override val outputType: KType = func.returnType.unwrapAsyncType()
    override val arguments: MutableList<Argument> = mutableListOf()

    init {
        for (it in func.valueParameters) {
            arguments += it.createArgument(context)
        }
    }

    override val dataFetcher: DataFetcher<Any?> = functionFetcher(func, arguments, receiver = instance)
}
