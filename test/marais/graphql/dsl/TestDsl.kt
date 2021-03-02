package marais.graphql.dsl

import graphql.GraphQL
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.SchemaPrinter
import marais.graphql.generator.SchemaGenerator
import kotlin.test.Test
import kotlin.test.assertTrue

class TestDsl {
    @ExperimentalStdlibApi
    @Test
    fun testDsl() {
        val startTime = System.currentTimeMillis()
        val builder = SchemaGenerator {

            scalar("Url", UrlCoercing)

            enum<MyEnum>()

            input<Input>()

            inter<Node> {
                derive()
            }

            type<MyData> {
                // TODO specifying interface on a type should automatically declare appropriate fields
                inter<Node>()

                // Can be a property
                field(MyData::id)
                // Can be a member function
                field(MyData::dec)
                field(MyData::field)
                // Can be a custom function
                field("inc") { it: DataFetchingEnvironment ->
                    field + 1
                }
            }

            type<OtherData> {
                inter<Node>()

                derive()

                field("custom") { param: String ->
                    param
                }

                field("custom2") { a: Int, b: Int ->
                    a * b
                }

                field("custom3") { a: Int, b: Float, c: String ->
                    "$c: ${a * b}"
                }
            }

            query(Query) { derive() }
        }
        val initTime = System.currentTimeMillis() - startTime
        val schema = builder.build()
        val buildTime = System.currentTimeMillis() - startTime
        println("Schema build time : $buildTime ms (of init : $initTime ms)")

        println(SchemaPrinter(SchemaPrinter.Options.defaultOptions()).print(schema))

        val graphql = GraphQL.newGraphQL(schema).build()
        val result = graphql.execute("""
            query {
              data { id, field, dec, inc },
              otherdata { id, field, custom(param: "hello") },
              node { id, 
                ... on MyData { value: field },
                ... on OtherData { url: field }
              },
              testSuspend,
              testDeferred,
              testFuture
            }
        """.trimIndent())
        println(result.getData<Map<String, Any?>>())
        assertTrue(result.errors.isEmpty(), "${result.errors}")
        println(result.extensions)
    }
}