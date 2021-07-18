package marais.graphql.dsl.test

import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import marais.graphql.dsl.SchemaBuilder
import marais.graphql.dsl.SchemaSpec
import marais.graphql.dsl.print
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val log = LoggerFactory.getLogger("marais.graphql.dsl.test.Helpers")

fun withSchema(schemaBuilder: SchemaSpec.() -> Unit, block: SchemaTestContext.() -> Unit) {
    val schema = SchemaBuilder(schemaBuilder).build()
    log.debug(schema.print(includeDirectives = false))
    block(SchemaTestContext(GraphQL.newGraphQL(schema).build(), schema))
}

class SchemaTestContext(val graphQL: GraphQL, val schema: GraphQLSchema) {
    fun withQuery(query: String, block: ExecutionResult.() -> Unit) {
        block(graphQL.execute(query))
    }

    fun assertQueryReturns(query: String, expected: Map<String, Any?>) {
        withQuery(query) {
            assertTrue(errors.isEmpty(), errors.toString())
            assertEquals(expected, getData())
        }
    }

    infix fun String.shouldReturns(expected: Map<String, Any?>) {
        assertQueryReturns(this, expected)
    }
}
