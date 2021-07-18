package marais.graphql.dsl

import graphql.schema.DataFetchingEnvironment
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Benchmark {

    private val log = LoggerFactory.getLogger(Benchmark::class.java)

    @Test
    fun testDsl() {
        val startTime = System.currentTimeMillis()
        val builder = SchemaBuilder {

            scalar(UrlCoercing, "Url")
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

            query(Query)
        }
        val initTime = System.currentTimeMillis() - startTime
        val schema = builder.build()
        val buildTime = System.currentTimeMillis() - startTime
        log.debug("Schema build time : $buildTime ms (init : $initTime ms)")
    }
}
