package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

@DslMarker
annotation class SchemaDsl

typealias IdConverter<T> = (value: String?) -> T?

internal val log = LoggerFactory.getLogger(SchemaSpec::class.java)

@SchemaDsl
class SchemaSpec : DescriptionPublisher {

    val idTypes = mutableMapOf<KClass<*>, IdConverter<*>>()
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

    // For the DescriptionPublisher implementation
    override var nextDesc: String? = null

    @SchemaDsl
    inline fun <reified T : Any> scalar(
        name: String,
        coercing: Coercing<T, *>,
        noinline builder: GraphQLScalarType.Builder.() -> Unit = {}
    ) {
        scalars += ScalarBuilder(name, T::class, coercing, takeDesc(), builder)
    }

    /**
     * Use the given class as the GraphQL ID
     *
     * @param converter since graphql will parse the field as a string we need a
     *                  manual conversion to our type to coerce it in input position
     */
    @SchemaDsl
    inline fun <reified T : Any> id(noinline converter: IdConverter<T>) {
        idTypes += T::class to converter
    }

    @SchemaDsl
    inline fun <reified T : Enum<T>> enum(
        name: String? = null,
        noinline builder: GraphQLEnumType.Builder.() -> Unit = {}
    ) {
        val kclass = T::class
        enums += EnumBuilder(name ?: kclass.simpleName!!, kclass, takeDesc(), builder)
    }

    @SchemaDsl
    inline fun <reified T : Any> input(
        name: String? = null,
        noinline builder: GraphQLInputObjectType.Builder.() -> Unit = {}
    ) {
        val kclass = T::class
        inputs += InputBuilder(name ?: kclass.simpleName!!, kclass, takeDesc(), builder)
    }

    @SchemaDsl
    inline fun <reified T : Any> inter(
        name: String? = null,
        configure: InterfaceBuilder<T>.() -> Unit = { derive() }
    ) {
        val kclass = T::class
        interfaces += InterfaceBuilder(kclass, name, takeDesc(), idTypes).apply(configure)
    }

    @SchemaDsl
    inline fun <reified T : Any> type(
        name: String? = null,
        configure: TypeBuilder<T>.() -> Unit = { derive() }
    ) {
        val kclass = T::class
        types += TypeBuilder(kclass, name, takeDesc(), idTypes).apply(configure)
    }

    @SchemaDsl
    inline fun <T : Any> query(query: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        this.query = OperationBuilder("Query", query, idTypes).apply(configure)
    }

    @SchemaDsl
    inline fun <T : Any> mutation(query: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        this.mutation = OperationBuilder("Mutation", query, idTypes).apply(configure)
    }

    @SchemaDsl
    inline fun <T : Any> subscription(query: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        this.subscription = OperationBuilder("Subscription", query, idTypes).apply(configure)
    }
}
