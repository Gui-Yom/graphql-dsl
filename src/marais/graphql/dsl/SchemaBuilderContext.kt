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

    internal var nextDesc: String? = null
    internal val nextArgDesc: MutableMap<String, String> = HashMap()

    @PublishedApi
    internal fun takeDesc(): String? {
        val desc = nextDesc
        nextDesc = null
        return desc
    }

    @PublishedApi
    internal fun takeArgDesc(arg: String): String? {
        val desc = nextArgDesc[arg]
        nextArgDesc.remove(arg)
        return desc
    }

    @PublishedApi
    internal fun checkRemainingArgDesc() {
        if (nextArgDesc.isNotEmpty()) {
            for (e in nextArgDesc) {
                log.warn("Unused argument description (Maybe a typo ?) : ${e.key}")
            }
            nextArgDesc.clear()
        }
    }

    internal val logDerive = LogManager.getLogger("${log.name}.derive")

    internal var convertFlowToPublisher = true

    internal fun getInputType(kclass: KClass<*>) = inputs.find { it.kclass == kclass }

    internal fun isInputType(kclass: KClass<*>) = getInputType(kclass) != null

    internal fun getImplementedInterfaces(kclass: KClass<*>): List<KClass<*>> = interfaces.filter {
        kclass.isSubclassOf(it.kclass)
    }.map { it.kclass }
}
