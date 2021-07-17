package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

@DslMarker
annotation class SchemaDsl

/**
 * Called when converting a string input to your Id class
 */
typealias IdCoercer<T> = (value: String?) -> T?

internal val log = LoggerFactory.getLogger(SchemaSpec::class.java)

@SchemaDsl
class SchemaSpec : DescriptionPublisher {

    val idTypes = mutableMapOf<KClass<*>, IdCoercer<*>>()
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
     * @param coercer since graphql will parse the field as a string we need a
     *                  manual conversion to our type to coerce it in input position
     */
    @SchemaDsl
    inline fun <reified T : Any> id(noinline coercer: IdCoercer<T>? = null) {
        // Ahem, yes i wrote that
        val coercer =
            coercer ?: (T::class.constructors.find { it.parameters[0].type == typeOf<String>() }
                ?: throw Exception("Can't find an appropriate constructor for ${T::class.simpleName} since no coercer was passed"))
                .let { constructor -> { it?.let { constructor.call(it) } } }
        idTypes += T::class to coercer
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
    inline fun query(configure: OperationBuilder<Any>.() -> Unit) {
        query(object {}, configure)
    }

    @SchemaDsl
    inline fun <T : Any> mutation(mutation: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        this.mutation = OperationBuilder("Mutation", mutation, idTypes).apply(configure)
    }

    @SchemaDsl
    inline fun mutation(configure: OperationBuilder<Any>.() -> Unit) {
        mutation(object {}, configure)
    }

    @SchemaDsl
    inline fun <T : Any> subscription(subscription: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        this.subscription = OperationBuilder("Subscription", subscription, idTypes).apply(configure)
    }

    @SchemaDsl
    inline fun subscription(configure: OperationBuilder<Any>.() -> Unit) {
        subscription(object {}, configure)
    }
}
