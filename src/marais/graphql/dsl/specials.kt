package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

data class ScalarBuilder(
    val kclass: KClass<*>,
    val name: String,
    val coercing: Coercing<*, *>,
    val description: String?,
    val builder: GraphQLScalarType.Builder.() -> Unit
)

data class EnumBuilder(
    val kclass: KClass<*>,
    val name: String,
    val description: String?,
    val builder: GraphQLEnumType.Builder.() -> Unit
)

data class InputBuilder(
    val kclass: KClass<*>,
    val name: String,
    val description: String?,
    val builder: GraphQLInputObjectType.Builder.() -> Unit
) {
    internal val constructor: KFunction<*> =
        kclass.primaryConstructor ?: throw Exception("Can't find a primary constructor for ${kclass.deriveName()}")
    internal val fields = constructor.parameters.map { it.name!! to it.type }
}
