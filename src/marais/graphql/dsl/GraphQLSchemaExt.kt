package marais.graphql.dsl

import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLSchema
import graphql.schema.idl.SchemaPrinter
import java.util.function.Predicate

fun GraphQLSchema.print(
    includeIntrospectionTypes: Boolean = false,
    includeScalarTypes: Boolean = true,
    includeDefaultSchemaDefinition: Boolean = true,
    includeDirectives: Boolean = true,
    includeDirectivesFilter: Predicate<GraphQLDirective> = Predicate { includeDirectives },
    includeDirectiveDefinitions: Boolean = true
): String {
    val schemaPrinter = SchemaPrinter(
        SchemaPrinter.Options.defaultOptions()
            .includeIntrospectionTypes(includeIntrospectionTypes)
            .includeScalarTypes(includeScalarTypes)
            .includeSchemaDefinition(includeDefaultSchemaDefinition)
            .includeDirectives(includeDirectives)
            .includeDirectives(includeDirectivesFilter)
            .includeDirectiveDefinitions(includeDirectiveDefinitions)
    )
    return schemaPrinter.print(this)
}
