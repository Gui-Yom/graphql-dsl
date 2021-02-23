package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@DslMarker
annotation class SchemaDsl

@SchemaDsl
class SchemaBuilder {

    val scalars = mutableListOf<Scalar<*>>()

    // Maps a kotlin type to a graphql interface declaration
    val interfaces = mutableMapOf<KType, InterfaceBuilder<*>>()

    // Maps a kotlin type to a graphql type declaration
    val types = mutableMapOf<KType, TypeBuilder<*>>()

    lateinit var query: QueryBuilder
    var mutation: MutationBuilder? = null
    var subscription: SubscriptionBuilder? = null

    @ExperimentalStdlibApi
    inline fun <reified T> scalar(
        name: String,
        coercing: Coercing<T, String>,
        noinline builder: GraphQLScalarType.Builder.() -> Unit = {}
    ): Scalar<T> {
        val scalar = Scalar(name, typeOf<T>(), coercing, builder)
        scalars += scalar
        return scalar
    }

    @ExperimentalStdlibApi
    inline fun <reified T : Any> inter(
        name: String? = null,
        configure: InterfaceBuilder<T>.() -> Unit = {}
    ): InterfaceBuilder<T> {
        val inter = InterfaceBuilder(T::class, name).apply(configure)
        interfaces[typeOf<T>()] = inter
        return inter
    }

    @ExperimentalStdlibApi
    inline fun <reified T : Any> type(
        name: String? = null,
        configure: TypeBuilder<T>.() -> Unit = {}
    ): TypeBuilder<T> {
        val type = TypeBuilder(T::class, name).apply(configure)
        types[typeOf<T>()] = type
        return type
    }

    fun query(configure: QueryBuilder.() -> Unit) {
        query = QueryBuilder().apply(configure)
    }

    fun mutation(configure: MutationBuilder.() -> Unit) {
        mutation = MutationBuilder().apply(configure)
    }

    fun subscription(configure: SubscriptionBuilder.() -> Unit) {
        subscription = SubscriptionBuilder().apply(configure)
    }
}

data class Scalar<T>(
    val name: String,
    val type: KType,
    val coercing: Coercing<T, *>,
    val builder: GraphQLScalarType.Builder.() -> Unit = {}
)
