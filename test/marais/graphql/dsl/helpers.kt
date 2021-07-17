package marais.graphql.dsl

import graphql.GraphQL
import graphql.schema.GraphQLSchema

fun withSchema(schemaBuilder: SchemaSpec.() -> Unit, block: GraphQL.(schema: GraphQLSchema) -> Unit) {
    val schema = SchemaBuilder(schemaBuilder).build()
    block(GraphQL.newGraphQL(schema).build(), schema)
}
