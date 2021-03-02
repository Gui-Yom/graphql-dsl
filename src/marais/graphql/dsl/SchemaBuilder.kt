package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType

@DslMarker
annotation class SchemaDsl

@SchemaDsl
class SchemaBuilder {

    val scalars = mutableListOf<Scalar>()
    val enums = mutableListOf<Enum>()

    // Maps a kotlin type to a graphql input type declaration
    val inputs = mutableListOf<Input>()

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
            coercing: Coercing<T, String>,
            noinline builder: GraphQLScalarType.Builder.() -> Unit = {}
    ) {
        scalars += Scalar(name, T::class, coercing, builder)
    }

    @SchemaDsl
    inline fun <reified T> enum(
            name: String? = null,
            noinline builder: GraphQLEnumType.Builder.() -> Unit = {}
    ) {
        val kclass = T::class
        enums += Enum(name ?: kclass.simpleName!!, kclass, builder)
    }

    @SchemaDsl
    inline fun <reified T> input(
            name: String? = null,
            noinline builder: GraphQLInputObjectType.Builder.() -> Unit = {}
    ) {
        val kclass = T::class
        inputs += Input(name ?: kclass.simpleName!!, kclass, builder)
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
        this.query = OperationBuilder("Mutation", query).apply(configure)
    }

    @SchemaDsl
    fun <T : Any> subscription(query: T, configure: OperationBuilder<T>.() -> Unit) {
        this.query = OperationBuilder("Subscription", query).apply(configure)
    }
}
