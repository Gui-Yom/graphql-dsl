package marais.graphql.dsl

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

/**
 * Description handling for a DSL context
 */
internal interface DescriptionDsl {

    val context: SchemaBuilderContext

    /**
     * Set the next element description
     */
    @SchemaDsl
    operator fun String.not() {
        val trimmed = this.trimIndent()
        val result = argDescPattern.matchEntire(trimmed)
        if (result != null) {
            if (result.groupValues[1] in context.nextArgDesc) {
                context.log.warn("Overwritten description element for argument '${result.groupValues[1]}' (probably unintentional)")
            }
            context.nextArgDesc[result.groupValues[1]] = result.groupValues[2]
        } else {
            if (context.nextDesc != null) {
                context.log.warn("Overwritten description element (probably unintentional), old desc: ${context.nextDesc}")
            }
            context.nextDesc = trimmed
        }
    }
}

private val argDescPattern = Regex("^(\\w+)\\s*:\\s*(.+)\$", RegexOption.DOT_MATCHES_ALL)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class GraphQLDescription(val desc: String)

/**
 * Extract the graphql description string from the [GraphQLDescription] annotation if present or null
 */
fun KAnnotatedElement.extractDesc(): String? = findAnnotation<GraphQLDescription>()?.desc?.trimIndent()
