package marais.graphql.dsl

import graphql.schema.DataFetcher
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect

sealed class FieldSpec(val name: String, val description: String?) {

    /**
     * Field arguments as displayed in the schema, no special types
     */
    abstract val arguments: List<Argument>

    /**
     * The code behind this field returning a value
     */
    abstract val dataFetcher: DataFetcher<Any>
    abstract val outputType: KType
}

class CustomFieldSpec(
    name: String,
    description: String?,
    override val outputType: KType,
    override val arguments: List<Argument>,
    override val dataFetcher: DataFetcher<Any>
) : FieldSpec(name, description)

internal class SuspendLambdaFieldSpec(
    name: String,
    description: String?,
    lambda: Function<*>,
    arity: Int,
    context: SchemaBuilderContext,
    reflected: KFunction<*> = lambda.reflect()!!
) : FieldSpec(name, description) {

    override val outputType: KType = reflected.returnType.unwrapAsyncType()
    override val arguments: MutableList<Argument> = mutableListOf(StaticArgument(lambda))

    init {
        reflected.valueParameters.mapTo(arguments) {
            it.createArgument(context)
        }
    }

    override val dataFetcher: DataFetcher<Any> =
        Lambdas.indirectCallSuspend(arity).fetcher(reflected.returnType, arguments)
}

internal class PropertyFieldSpec<R>(
    property: KProperty1<R, Any?>,
    name: String,
    description: String?,
    instance: R? = null
) : FieldSpec(name, description ?: property.extractDesc()) {

    override val dataFetcher: DataFetcher<Any> = property.getter.fetcher(property.returnType, emptyList())
    override val outputType: KType = property.returnType.unwrapAsyncType()
    override val arguments: List<Argument> = emptyList()
}

internal class FunctionFieldSpec<R>(
    func: KFunction<Any?>,
    name: String,
    description: String?,
    instance: R?,
    context: SchemaBuilderContext
) : FieldSpec(name, description ?: func.extractDesc()) {

    override val outputType: KType = func.returnType.unwrapAsyncType()
    override val arguments: MutableList<Argument> = mutableListOf()

    init {
        for (it in func.valueParameters) {
            arguments += it.createArgument(context)
        }
    }

    override val dataFetcher: DataFetcher<Any> = func.fetcher(func.returnType, arguments)
}
