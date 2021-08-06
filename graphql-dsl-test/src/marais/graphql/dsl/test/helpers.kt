package marais.graphql.dsl.test

import graphql.ExecutionResult
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import marais.graphql.dsl.GraphQLSchema
import marais.graphql.dsl.SchemaSpec
import marais.graphql.dsl.print
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Construct a GraphQL engine following the given [SchemaSpec].
 *
 * @param schemaSpec the DSL to build the schema
 * @param builder the graphql engine builder
 * @param block the code to run with the [SchemaTestContext] as receiver
 */
fun withSchema(
    schemaSpec: SchemaSpec.() -> Unit,
    builder: GraphQL.Builder.() -> Unit,
    block: SchemaTestContext.() -> Unit
) {
    val schema = GraphQLSchema(schemaSpec)
    println(schema.print(includeDirectives = false))
    block(SchemaTestContext(GraphQL.newGraphQL(schema).apply(builder).build(), schema))
}

/**
 * Construct a GraphQL engine following the given [SchemaSpec].
 *
 * @param schemaSpec the DSL to build the schema
 * @param block the code to run with the [SchemaTestContext] as receiver
 */
fun withSchema(schemaSpec: SchemaSpec.() -> Unit, block: SchemaTestContext.() -> Unit) =
    withSchema(schemaSpec, {}, block)

/**
 * Assert the given schema fails.
 */
fun assertSchemaFails(schemaSpec: SchemaSpec.() -> Unit) {
    assertFails {
        GraphQLSchema(schemaSpec)
    }
}

class SchemaTestContext(val graphQL: GraphQL, val schema: GraphQLSchema) {
    /**
     * Executes [query] in the GraphQL engine and call [block] with the [ExecutionResult] as receiver.
     *
     * @param query the GraphQL query to execute
     * @param block the code to run
     */
    fun withQuery(query: String, block: ExecutionResult.() -> Unit) {
        block(graphQL.execute(query))
    }

    /**
     * Executes [query] in the GraphQL engine, check that no errors are returned and compare the results with [expected].
     *
     * @param query the GraphQL query to run
     * @param expected the expected result
     */
    fun <T> assertQueryReturns(query: String, expected: T?) {
        withQuery(query) {
            assertTrue(errors.isEmpty(), errors.toString())
            assertEquals(expected, getData())
        }
    }

    /**
     * Executes this String as a query in the GraphQL engine, check that no errors are returned and compare the results with [expected].
     *
     * @param expected the expected result
     */
    infix fun <T> String.shouldReturns(expected: T?) {
        assertQueryReturns(this, expected)
    }

    inline fun <reified T : Throwable> assertQueryFailsWith(query: String) {
        assertFailsWith<T> {
            graphQL.execute(query)
        }
    }
}
