package marais.graphql.dsl

import graphql.language.StringValue
import graphql.schema.*
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDsl {
    private val log = LoggerFactory.getLogger(TestDsl::class.java)

    @Test
    fun testSimpleSchema() = withSchema({
        query {
            "answer" { -> 42 }
        }
    }) {
        assertEquals(mapOf("answer" to 42), execute("""query { answer }""").getData())
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
        }) { schema ->
            assertContains(schema.print(includeDirectives = false), "scalar Handle")
            assertEquals(
                mapOf("test" to "hello"),
                execute("""query { test(handle: "hello") }""").getData()
            )
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
            assertEquals(mapOf("node" to "hello"), execute("""query { node(id: "hello") }""").getData())
        }
    }

    @Test
    fun testEnum() = withSchema({
        enum<Baz>()

        query {
            "test" { -> Baz.VALUE0 }
        }
    }) { schema ->
        assertContains(schema.typeMap, "Baz")
        assertEquals(3, (schema.typeMap["Baz"] as GraphQLEnumType).values.size)
    }

    @Test
    fun testInput() {
        data class MyInput(
            val data: String,
            val other: Int
        )

        withSchema({
            input<MyInput>()

            query {
                "test" { input: MyInput -> "$input.data $input.other" }
            }
        }) {
            val result = execute("""query { test(input: { data: "hello", other: 42 }) }""")
            println(result.errors)
            assertTrue(result.errors.isEmpty())
            assertEquals(
                mapOf("test" to "hello 42"),
                result.getData()
            )
        }
    }

    @Test
    fun testSimpleMap() = withSchema({
        query(object {
            val prop = mapOf("key" to "value")
        })
    }) {
        assertEquals(
            mapOf("prop" to listOf(mapOf("key" to "key", "value" to "value"))),
            execute("query { prop { key, value } }").getData()
        )
    }
}