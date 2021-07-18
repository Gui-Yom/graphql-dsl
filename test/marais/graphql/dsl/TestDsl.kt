package marais.graphql.dsl

import graphql.language.StringValue
import graphql.schema.*
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TestDsl {
    private val log = LoggerFactory.getLogger(TestDsl::class.java)

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
            scalar("Handle", object : Coercing<Handle, String> {
                override fun serialize(dataFetcherResult: Any?): String {
                    return when (dataFetcherResult) {
                        is Handle -> dataFetcherResult.inner
                        else -> throw CoercingSerializeException("Must be Handle")
                    }
                }

                override fun parseValue(input: Any?): Handle {
                    return when (input) {
                        is String -> Handle(input)
                        else -> throw CoercingParseValueException("Must be String")
                    }
                }

                override fun parseLiteral(input: Any?): Handle {
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
        query(object {
            val prop = mapOf("key" to "value")
        })
    }) {
        """query { prop { key, value } }""" shouldReturns mapOf(
            "prop" to listOf(
                mapOf(
                    "key" to "key",
                    "value" to "value"
                )
            )
        )
    }
}