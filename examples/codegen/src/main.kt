import marais.graphql.codegen.GraphQLSchemaCodegen
import marais.graphql.dsl.GraphQLSchema

object SchemaProvider {

    @GraphQLSchemaCodegen
    val schema = GraphQLSchema {

    }
}

fun main(args: Array<String>) {
    println(GeneratedSchema.logs)
}
