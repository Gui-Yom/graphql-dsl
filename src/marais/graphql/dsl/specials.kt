package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

data class Scalar(
        val name: String,
        val kclass: KClass<*>,
        val coercing: Coercing<*, *>,
        val builder: GraphQLScalarType.Builder.() -> Unit
)

data class Enum(
        val name: String,
        val kclass: KClass<*>,
        val builder: GraphQLEnumType.Builder.() -> Unit
)

data class Input(
        val name: String,
        val kclass: KClass<*>,
        val builder: GraphQLInputObjectType.Builder.() -> Unit
) {
    val fields = mutableListOf<Pair<String, KType>>()

    init {
        for (memberProperty in kclass.memberProperties) {
            fields += memberProperty.name to memberProperty.returnType
        }
    }
}
