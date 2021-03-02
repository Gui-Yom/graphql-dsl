package marais.graphql.dsl

import graphql.schema.GraphQLInputObjectType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

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
