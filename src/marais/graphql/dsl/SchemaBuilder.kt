package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLScalarType
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.declaredMembers

@DslMarker
annotation class SchemaDsl

@SchemaDsl
class SchemaBuilder {

    val scalars = mutableListOf<Scalar<*>>()
    val enums = mutableSetOf<Enum>()

    // Maps a kotlin type to a graphql interface declaration
    val interfaces = mutableMapOf<KClass<*>, InterfaceBuilder<*>>()

    // Maps a kotlin type to a graphql type declaration
    val types = mutableMapOf<KClass<*>, TypeBuilder<*>>()

    // Maps a kotlin type to a graphql input type declaration
    val inputs = mutableMapOf<KClass<*>, Input>()

    lateinit var query: OperationBuilder<*>
    var mutation: OperationBuilder<*>? = null
    var subscription: OperationBuilder<*>? = null

    @SchemaDsl
    inline fun <reified T : Any> scalar(
            name: String,
            coercing: Coercing<T, String>,
            noinline builder: GraphQLScalarType.Builder.() -> Unit = {}
    ) {
        val scalar = Scalar(name, T::class, coercing, builder)
        scalars += scalar
    }

    @SchemaDsl
    inline fun <reified T> enum(
            name: String? = null,
            noinline builder: GraphQLEnumType.Builder.() -> Unit = {}
    ) {
        val kclass = T::class
        val enum = Enum(name ?: kclass.simpleName!!, kclass, builder)
        enums += enum
    }

    @SchemaDsl
    inline fun <reified T : Any> inter(
            name: String? = null,
            configure: InterfaceBuilder<T>.() -> Unit = {}
    ) {
        val kclass = T::class
        val inter = InterfaceBuilder(kclass, name).apply(configure)
        interfaces[kclass] = inter
    }

    @SchemaDsl
    inline fun <reified T : Any> type(
            name: String? = null,
            configure: TypeBuilder<T>.() -> Unit = {}
    ) {
        val kclass = T::class
        val type = TypeBuilder(kclass, name).apply(configure)
        types[kclass] = type
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

data class Scalar<T : Any>(
        val name: String,
        val kclass: KClass<T>,
        val coercing: Coercing<T, *>,
        val builder: GraphQLScalarType.Builder.() -> Unit
)

data class Enum(
        val name: String,
        val kclass: KClass<*>,
        val builder: GraphQLEnumType.Builder.() -> Unit
)
