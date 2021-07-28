package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

class ScalarSpec(
    val kclass: KClass<*>,
    val name: String,
    val coercing: Coercing<*, *>,
    description: String?,
    val builder: GraphQLScalarType.Builder.() -> Unit
) {
    val description: String? = description ?: kclass.extractDesc()
}

class EnumSpec(
    val kclass: KClass<*>,
    val name: String,
    description: String?,
    val builder: GraphQLEnumType.Builder.() -> Unit
) {
    val description: String? = description ?: kclass.extractDesc()
}

class InputSpec(
    val kclass: KClass<*>,
    val name: String,
    description: String?,
    val builder: GraphQLInputObjectType.Builder.() -> Unit
) {
    val description: String? = description ?: kclass.extractDesc()

    internal val constructor: KFunction<*> =
        kclass.primaryConstructor ?: throw Exception("Can't find a primary constructor for ${kclass.deriveName()}")
    internal val fields = constructor.parameters.map { it.name!! to it.type }
}
