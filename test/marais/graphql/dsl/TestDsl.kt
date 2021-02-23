package marais.graphql.dsl

import graphql.GraphQL
import graphql.schema.idl.SchemaPrinter
import marais.graphql.generator.SchemaGenerator
import java.net.URL
import kotlin.random.Random
import kotlin.test.Test

class TestDsl {
    @ExperimentalStdlibApi
    @Test
    fun testDsl() {
        val startTime = System.currentTimeMillis()
        val builder = SchemaGenerator {

            scalar("Url", UrlCoercing)

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
                field("inc") {
                    field + 1
                }
            }

            type<OtherData> {
                inter<Node>()

                derive()
            }

            query {
                val data = MyData("69420", 42)
                val otherdata = OtherData("42069", URL("http://localhost:8080"))

                field("data") {
                    data
                }

                field("node") {
                    if (Random.nextBoolean()) data else otherdata
                }

                field("otherdata") {
                    otherdata
                }
            }
        }
        val initTime = System.currentTimeMillis() - startTime
        val schema = builder.build()
        val buildTime = System.currentTimeMillis() - startTime
        println("Schema build time : $buildTime ms (of init : $initTime ms)")

        println(SchemaPrinter(SchemaPrinter.Options.defaultOptions()).print(schema))

        val graphql = GraphQL.newGraphQL(schema).build()
        val result = graphql.execute(
            """
            query {
              data { id, field, dec, inc },
              otherdata { id, field },
              node { id, 
                ... on MyData { value: field },
                ... on OtherData { url: field }
              } 
            }
            """.trimIndent()
        )
        println(result.getData<Map<String, Any?>>())
        println(result.errors)
        println(result.extensions)
    }
}