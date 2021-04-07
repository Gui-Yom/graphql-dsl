package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KClass

@DslMarker
annotation class SchemaDsl

@SchemaDsl
class SchemaSpec {

    val idTypes = mutableSetOf<KClass<*>>()
    val scalars = mutableListOf<ScalarBuilder>()
    val enums = mutableListOf<EnumBuilder>()

    // Maps a kotlin type to a graphql input type declaration
    val inputs = mutableListOf<InputBuilder>()

    // Maps a kotlin type to a graphql interface declaration
    val interfaces = mutableListOf<InterfaceBuilder<*>>()

    // Maps a kotlin type to a graphql type declaration
    val types = mutableListOf<TypeBuilder<*>>()

    lateinit var query: OperationBuilder<*>
    var mutation: OperationBuilder<*>? = null
    var subscription: OperationBuilder<*>? = null

    @SchemaDsl
    inline fun <reified T : Any> scalar(
        name: String,
        coercing: Coercing<T, *>,
        noinline builder: GraphQLScalarType.Builder.() -> Unit = {}
    ) {
        scalars += ScalarBuilder(name, T::class, coercing, builder)
    }

    @SchemaDsl
    inline fun <reified T : Any> id() {
        idTypes += T::class
    }

    @SchemaDsl
    inline fun <reified T : Enum<T>> enum(
        name: String? = null,
        noinline builder: GraphQLEnumType.Builder.() -> Unit = {}
    ) {
        val kclass = T::class
        enums += EnumBuilder(name ?: kclass.simpleName!!, kclass, builder)
    }

    @SchemaDsl
    inline fun <reified T : Any> input(
        name: String? = null,
        noinline builder: GraphQLInputObjectType.Builder.() -> Unit = {}
    ) {
        val kclass = T::class
        inputs += InputBuilder(name ?: kclass.simpleName!!, kclass, builder)
    }

    @SchemaDsl
    inline fun <reified T : Any> inter(
        name: String? = null,
        configure: InterfaceBuilder<T>.() -> Unit = {}
    ) {
        val kclass = T::class
        interfaces += InterfaceBuilder(kclass, name).apply(configure)
    }

    @SchemaDsl
    inline fun <reified T : Any> type(
        name: String? = null,
        configure: TypeBuilder<T>.() -> Unit = {}
    ) {
        val kclass = T::class
        types += TypeBuilder(kclass, name).apply(configure)
    }

    @SchemaDsl
    fun <T : Any> query(query: T, configure: OperationBuilder<T>.() -> Unit) {
        this.query = OperationBuilder("Query", query).apply(configure)
    }

    @SchemaDsl
    fun <T : Any> mutation(query: T, configure: OperationBuilder<T>.() -> Unit) {
        this.mutation = OperationBuilder("Mutation", query).apply(configure)
    }

    @SchemaDsl
    fun <T : Any> subscription(query: T, configure: OperationBuilder<T>.() -> Unit) {
        this.subscription = OperationBuilder("Subscription", query).apply(configure)
    }
}
