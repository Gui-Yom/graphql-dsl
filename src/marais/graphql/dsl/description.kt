package marais.graphql.dsl

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

/**
 * Allow a root DSL to hold the description operator
 */
interface DescriptionHolder {
    var nextDesc: String?

    /**
     * Set the next element description
     */
    operator fun String.not() {
        nextDesc = this.trimIndent()
    }

    fun takeDesc(): String? {
        val desc = nextDesc
        nextDesc = null
        return desc
    }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class GraphQLDescription(val desc: String)

/**
 * Extract the graphql description string from the [GraphQLDescription] annotation if present or null
 */
fun KAnnotatedElement.extractDesc(): String? = findAnnotation<GraphQLDescription>()?.desc
