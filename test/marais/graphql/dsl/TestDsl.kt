package marais.graphql.dsl

import graphql.GraphQL
import graphql.schema.DataFetchingEnvironment
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestDsl {
    private val log = LoggerFactory.getLogger(TestDsl::class.java)

    // TODO make more smaller test

    @Test
    fun testDsl() {
        val startTime = System.currentTimeMillis()
        val builder = SchemaBuilder {

            scalar("Url", UrlCoercing)
            id<MyId> {
                it?.let { MyId(it) }
            }

            enum<Baz>()

            input<Input>()

            !"This describes my Node interface"
            inter<Node> {
                derive()

                !"Description on a field too !"
                field("parent") { ->
                    MyId("Node:" + id.inner)
                }
            }

            !"""
                This is a cool looking multiline description
                No need to call .trimIndent()
            """
            type<Foo> {
                // TODO specifying interface on a type should automatically declare appropriate fields
                inter<Node>()

                // Can be a property
                include(Foo::id)
                // Can be a member function
                include(Foo::dec)
                assertFailsWith<Exception>("We already included that field with the same name") {
                    include(Foo::dec)
                }
                include(Foo::field)
                // Can be a custom function
                field("inc") { _: DataFetchingEnvironment ->
                    field + 1
                }
            }

            type<Bar> {
                inter<Node>()

                derive {
                    -Bar::field

                    assertFailsWith<Exception>("We already excluded this field") {
                        exclude(Bar::field)
                    }
                }

                field("custom") { param: String ->
                    param
                }

                field("custom2") { a: List<Int>, b: Int ->
                    a.map { it * b }
                }

                field("custom3") { a: Int, b: Float, c: MyId ->
                    "$c: ${a * b}"
                }
            }

            query(Query) { derive() }
        }
        val initTime = System.currentTimeMillis() - startTime
        val schema = builder.build()
        val buildTime = System.currentTimeMillis() - startTime
        log.debug("Schema build time : $buildTime ms (init : $initTime ms)")

        log.debug(schema.print(includeDirectives = false))

        val graphql = GraphQL.newGraphQL(schema).build()
        val result = graphql.execute(
            """
            query {
              foo {
                id,
                field,
                dec,
                inc,
                parent
              },
              bar {
                id,
                custom(param: "hello"),
                custom2(a: [1, 2, 3], b: 4)
              },
              node {
                id,
                parent,
                ... on Foo { value: field },
                ... on Bar { custom(param: "hello") }
              },
              suspendFun,
              deferedFun,
              futureFun,
              acceptId(id: "yay")
            }
        """.trimIndent()
        )
        log.debug(result.getData<Map<String, Any?>?>()?.toString())
        assertTrue(result.errors.isEmpty(), "${result.errors}")
        log.debug(result.extensions?.toString())
    }

    @Test
    fun testMap() {
        withSchema({
            query(object {
                val prop = mapOf("key" to "value")
            })
        }) { schema ->
            log.debug(schema.print(includeDirectives = false))
            assertEquals(
                mapOf("prop" to listOf(mapOf("key" to "key", "value" to "value"))),
                execute("query { prop { key, value } }").getData()
            )
        }
    }

    @Test
    fun testAutomaticIdCoercer() {

        data class MyId(val inner: String)

        withSchema({

            id<MyId>()

            query("object {}") {
                field("node") { id: MyId ->
                    id.inner
                }
            }
        }) { schema ->
            log.debug(schema.print(includeDirectives = false))

            assertEquals(mapOf("node" to "hello"), execute("""query { node(id: "hello") }""").getData())
        }
    }
}