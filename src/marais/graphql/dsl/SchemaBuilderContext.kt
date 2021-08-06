package marais.graphql.dsl

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Called when converting a string input to your Id class
 */
typealias IdCoercer<T> = (value: String?) -> T?

/**
 * Shared read-only context for all builders
 */
class SchemaBuilderContext(
    val log: Logger,
    val idCoercers: Map<KClass<*>, IdCoercer<*>>,
    val inputs: List<InputSpec>,
    val interfaces: List<InterfaceSpec<*>>
) {

    internal val logDerive = LogManager.getLogger("${log.name}.derive")

    internal var convertFlowToPublisher = true

    internal fun getInputType(kclass: KClass<*>) = inputs.find { it.kclass == kclass }

    internal fun isInputType(kclass: KClass<*>) = getInputType(kclass) != null

    internal fun getImplementedInterfaces(kclass: KClass<*>): List<KClass<*>> = interfaces.filter {
        kclass.isSubclassOf(it.kclass)
    }.map { it.kclass }
}
