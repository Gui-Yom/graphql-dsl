package marais.graphql.dsl

import graphql.schema.GraphQLObjectType
import marais.graphql.dsl.test.withSchema
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDescription {
    @Test
    fun `dsl description`() = withSchema({

        data class A(val data: String)

        !"On type"
        type<A>()

        query {
            !"On field"
            "test" { -> 42 }
        }
    }) {
        assertEquals("On field", schema.queryType.getFieldDefinition("test").description)
        assertEquals("On type", (schema.getType("A") as GraphQLObjectType).description)
    }

    @Test
    fun `annotation description`() = withSchema({

        @GraphQLDescription("On type")
        data class A(val data: String)

        val query = object {
            @GraphQLDescription("On function")
            fun test(@GraphQLDescription("On parameter") arg: Int) = arg

            @GraphQLDescription("On property")
            val test2: String
                get() = "const"
        }

        type<A>()

        query(query)
    }) {
        assertEquals("On function", schema.queryType.getFieldDefinition("test").description)
        assertEquals("On parameter", schema.queryType.getFieldDefinition("test").getArgument("arg").description)
        assertEquals("On property", schema.queryType.getFieldDefinition("test2").description)
        assertEquals("On type", (schema.getType("A") as GraphQLObjectType).description)
    }

    @Test
    fun `priority to dsl description`() = withSchema({

        @GraphQLDescription("Annotation")
        data class A(val data: String)

        val query = object {
            @GraphQLDescription("Annotation")
            fun test() = 42
        }

        !"dsl"
        type<A>()

        query(query) {
            !"dsl"
            +query::test
        }
    }) {
        assertEquals("dsl", schema.queryType.getFieldDefinition("test").description)
        assertEquals("dsl", (schema.getType("A") as GraphQLObjectType).description)
    }
}
