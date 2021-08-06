package marais.graphql.dsl

import graphql.AssertException
import graphql.language.StringValue
import graphql.schema.*
import kotlinx.coroutines.flow.flowOf
import marais.graphql.dsl.test.withSchema
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TestDsl {
    @Test
    fun testSimpleSchema() = withSchema({
        query {
            "answer" { -> 42 }
        }
    }) {
        """query { answer }""" shouldReturns mapOf("answer" to 42)
    }

    @Test
    fun testScalar() {

        data class Handle(val inner: String)

        withSchema({
            scalar(object : Coercing<Handle, String> {
                override fun serialize(dataFetcherResult: Any): String {
                    return when (dataFetcherResult) {
                        is Handle -> dataFetcherResult.inner
                        else -> throw CoercingSerializeException("Must be Handle")
                    }
                }

                override fun parseValue(input: Any): Handle {
                    return when (input) {
                        is String -> Handle(input)
                        else -> throw CoercingParseValueException("Must be String")
                    }
                }

                override fun parseLiteral(input: Any): Handle {
                    return when (input) {
                        is StringValue -> Handle(input.value)
                        else -> throw CoercingParseLiteralException("Must be String")
                    }
                }
            })

            query {
                "test" { handle: Handle -> handle }
            }
        }) {
            assertContains(schema.print(includeDirectives = false), "scalar Handle")
            """query { test(handle: "hello") }""" shouldReturns mapOf("test" to "hello")
        }
    }

    @Test
    fun testAutomaticIdCoercer() {

        data class MyId(val inner: String)

        withSchema({

            id<MyId>()

            query {
                "node" { id: MyId ->
                    id.inner
                }
            }
        }) {
            """query { node(id: "hello") }""" shouldReturns mapOf("node" to "hello")
        }
    }

    @Test
    fun testEnum() = withSchema({
        enum<Baz>()

        query {
            "test" { -> Baz.VALUE0 }
        }
    }) {
        assertContains(schema.typeMap, "Baz")
        assertEquals(3, (schema.typeMap["Baz"] as GraphQLEnumType).values.size)
    }

    @Test
    fun testSimpleMap() = withSchema({
        query {
            "test" { -> mapOf("key" to "value") }
        }
    }) {
        """query { test { key, value } }""" shouldReturns mapOf(
            "test" to listOf(
                mapOf(
                    "key" to "key",
                    "value" to "value"
                )
            )
        )
    }

    @Test
    fun `requires receiver derive`() = withSchema({
        // We need an instance of the anonymous object to access 'test'
        query(object {
            val test = 42
        })
    }) {
        """query { test }""" shouldReturns mapOf("test" to 42)
    }

    @Test
    fun `requires receiver include`() = withSchema({
        // We need an instance of the anonymous object to access 'test'

        class Query {
            val test = 42
        }

        query(Query()) {
            +Query::test
        }
    }) {
        """query { test }""" shouldReturns mapOf("test" to 42)
    }

    @Test
    fun `requires receiver additional field0`() = withSchema({
        // We need an instance of the anonymous object to access 'test'

        class Query {
            val test = 42
        }

        query(Query()) {
            derive()

            "test2" { -> test + 1 }
        }
    }) {
        """query { test, test2 }""" shouldReturns mapOf("test" to 42, "test2" to 43)
    }

    @Test
    fun `requires receiver additional field1`() = withSchema({
        // We need an instance of the anonymous object to access 'test'

        class Query {
            val test = 42
        }

        query(Query()) {
            derive()

            "test2" { arg: Int -> test + arg }
        }
    }) {
        """query { test, test2(arg: 1) }""" shouldReturns mapOf("test" to 42, "test2" to 43)
    }

    @Test
    fun `do not convert flow to publisher`() = withSchema({

        doNotConvertFlowToPublisher()

        query {
            "empty" { -> 0 }
        }

        subscription {
            "test" { -> flowOf("hello", "world") }
        }
    }) {
        // The default SubscriptionExecutionStrategy does not handle Flow and throws an Exception
        // This means we didn't convert the Flow to a Publisher
        assertQueryFailsWith<AssertException>("""subscription { test }""")
    }
}