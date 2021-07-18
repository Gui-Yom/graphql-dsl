package marais.graphql.dsl

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Schema")

fun withSchema(schemaBuilder: SchemaSpec.() -> Unit, block: GraphQL.(schema: GraphQLSchema) -> Unit) {
    val schema = SchemaBuilder(schemaBuilder).build()
    log.debug(schema.print(includeDirectives = false))
    block(GraphQL.newGraphQL(schema).build(), schema)
}
