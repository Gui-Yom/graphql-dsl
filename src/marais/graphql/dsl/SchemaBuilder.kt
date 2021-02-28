package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@DslMarker
annotation class SchemaDsl

@SchemaDsl
class SchemaBuilder {

    val scalars = mutableListOf<Scalar<*>>()
    val enums = mutableListOf<Enum>()

    // Maps a kotlin type to a graphql interface declaration
    val interfaces = mutableMapOf<KType, InterfaceBuilder<*>>()

    // Maps a kotlin type to a graphql type declaration
    val types = mutableMapOf<KType, TypeBuilder<*>>()

    lateinit var query: OperationBuilder<*>
    var mutation: OperationBuilder<*>? = null
    var subscription: OperationBuilder<*>? = null

    @ExperimentalStdlibApi
    @SchemaDsl
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
    @SchemaDsl
    inline fun <reified T> enum(
            name: String? = null,
            noinline builder: GraphQLEnumType.Builder.() -> Unit = {}
    ) {
        val type = typeOf<T>()
        val enum = Enum(name ?: T::class.simpleName!!, type, builder)
        enums += enum
    }

    @ExperimentalStdlibApi
    @SchemaDsl
    inline fun <reified T : Any> inter(
            name: String? = null,
            configure: InterfaceBuilder<T>.() -> Unit = {}
    ): InterfaceBuilder<T> {
        val inter = InterfaceBuilder(T::class, name).apply(configure)
        interfaces[typeOf<T>()] = inter
        return inter
    }

    @ExperimentalStdlibApi
    @SchemaDsl
    inline fun <reified T : Any> type(
            name: String? = null,
            configure: TypeBuilder<T>.() -> Unit = {}
    ): TypeBuilder<T> {
        val type = TypeBuilder(T::class, name).apply(configure)
        types[typeOf<T>()] = type
        return type
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

data class Scalar<T>(
        val name: String,
        val type: KType,
        val coercing: Coercing<T, *>,
        val builder: GraphQLScalarType.Builder.() -> Unit = {}
)

data class Enum(
        val name: String,
        val type: KType,
        val builder: GraphQLEnumType.Builder.() -> Unit
)
