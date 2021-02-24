package marais.graphql.dsl

import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.jvm.reflect

object Query

@SchemaDsl
class QueryBuilder : Type<Query>("Query") {
    inline fun <reified O> field(
        name: String,
        description: String? = null,
        noinline resolver: (DataFetchingEnvironment) -> O
    ) {
        // lmao
        fields += CustomField(name, description, resolver.reflect()!!.returnType, listOf(), resolver)
    }
}

object Mutation

@SchemaDsl
class MutationBuilder : Type<Mutation>("Mutation") {
    inline fun <reified O> field(
        name: String,
        description: String? = null,
        noinline resolver: (DataFetchingEnvironment) -> O
    ) {
        // lmao
        fields += CustomField(name, description, resolver.reflect()!!.returnType, listOf(), resolver)
    }
}

object Subscription

@SchemaDsl
class SubscriptionBuilder : Type<Subscription>("Subscription") {
    inline fun <reified O> field(
        name: String,
        description: String? = null,
        noinline resolver: (DataFetchingEnvironment) -> O
    ) {
        // lmao
        fields += CustomField(name, description, resolver.reflect()!!.returnType, listOf(), resolver)
    }
}