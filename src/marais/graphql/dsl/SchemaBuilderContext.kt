package marais.graphql.dsl

import org.apache.logging.log4j.Logger
import kotlin.reflect.KClass

/**
 * Called when converting a string input to your Id class
 */
typealias IdCoercer<T> = (value: String?) -> T?

/**
 * Shared read-only context for all builders
 */
abstract class SchemaBuilderContext(internal val log: Logger) {

    abstract val idCoercers: Map<KClass<*>, IdCoercer<*>>
    abstract val inputs: List<InputSpec>

    internal fun getInputType(kclass: KClass<*>) = inputs.find { it.kclass == kclass }

    internal fun isInputType(kclass: KClass<*>) = getInputType(kclass) != null
}
