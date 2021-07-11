package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

data class ScalarBuilder(
    val name: String,
    val kclass: KClass<*>,
    val coercing: Coercing<*, *>,
    val description: String?,
    val builder: GraphQLScalarType.Builder.() -> Unit
)

data class EnumBuilder(
    val name: String,
    val kclass: KClass<*>,
    val description: String?,
    val builder: GraphQLEnumType.Builder.() -> Unit
)

data class InputBuilder(
    val name: String,
    val kclass: KClass<*>,
    val description: String?,
    val builder: GraphQLInputObjectType.Builder.() -> Unit
) {
    val fields = mutableMapOf<String, KType>()

    init {
        for (memberProperty in kclass.memberProperties) {
            fields[memberProperty.name] = memberProperty.returnType
        }
    }
}
