package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import org.slf4j.Logger
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

@DslMarker
annotation class SchemaDsl

/**
 * Holds the root DSL.
 */
@SchemaDsl
class SchemaSpec(log: Logger) : SchemaBuilderContext(log), DescriptionHolder {

    override val idCoercers = mutableMapOf<KClass<*>, IdCoercer<*>>()
    val scalars = mutableListOf<ScalarBuilder>()
    val enums = mutableListOf<EnumBuilder>()

    // Maps a kotlin type to a graphql input type declaration
    override val inputs = mutableListOf<InputBuilder>()

    // Maps a kotlin type to a graphql interface declaration
    val interfaces = mutableListOf<InterfaceBuilder<*>>()

    // Maps a kotlin type to a graphql type declaration
    val types = mutableListOf<TypeBuilder<*>>()

    lateinit var query: OperationBuilder<*>
    var mutation: OperationBuilder<*>? = null
    var subscription: OperationBuilder<*>? = null

    // For the DescriptionPublisher implementation
    override var nextDesc: String? = null

    /**
     * Define [T] as being a GraphQL scalar.
     *
     * @param coercing the [Coercing] implementation for this scalar
     * @param name the name as displayed in the schema, defaults to the class name
     * @param builder a hook just before constructing the final type
     */
    @SchemaDsl
    inline fun <reified T : Any> scalar(
        coercing: Coercing<T, *>,
        name: String = T::class.deriveName(),
        noinline builder: GraphQLScalarType.Builder.() -> Unit = {}
    ) {
        scalars += ScalarBuilder(T::class, name, coercing, takeDesc(), builder)
    }

    /**
     * Use [T] as the GraphQL ID
     *
     * @param coercer since graphql will parse the field as a string we need manual conversion to our type to coerce it in input position. By default, this takes a constructor of [T] with a single [String] parameter.
     */
    @SchemaDsl
    inline fun <reified T : Any> id(noinline coercer: IdCoercer<T>? = null) {
        // Ahem, yes i wrote that
        val coercer =
            coercer ?: (T::class.constructors.find { it.parameters[0].type == typeOf<String>() }
                ?: throw Exception("Can't find an appropriate constructor for ${T::class.simpleName} since no coercer was passed"))
                .let { constructor -> { it?.let { constructor.call(it) } } }
        idCoercers += T::class to coercer
    }

    /**
     * Declare [T] as a GraphQL enum.
     *
     * @param name the name as displayed in the schema. Defaults to the class name.
     * @param builder a hook just before constructing the final type
     */
    @SchemaDsl
    inline fun <reified T : Enum<T>> enum(
        name: String = T::class.deriveName(),
        noinline builder: GraphQLEnumType.Builder.() -> Unit = {}
    ) {
        enums += EnumBuilder(T::class, name, takeDesc(), builder)
    }

    /**
     * Declare [T] as a GraphQL Input Object.
     * The ideal is for [T] to be a data class. The input object fields will be derived automatically from the primary constructor parameters.
     *
     * @param name the name as displayed in the schema. Defaults to the class name.
     * @param builder a hook just before constructing the final type
     */
    @SchemaDsl
    inline fun <reified T : Any> input(
        name: String = T::class.deriveName(),
        noinline builder: GraphQLInputObjectType.Builder.() -> Unit = {}
    ) {
        inputs += InputBuilder(T::class, name, takeDesc(), builder)
    }

    /**
     * Declare [T] as a GraphQL Interface.
     * [T] should be a Kotlin interface, sealed class or abstract class.
     *
     * @param name the name as displayed in the schema. Defaults to the class name.
     * @param configure the DSL to construct this interface. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <reified T : Any> inter(
        name: String = T::class.deriveName(),
        configure: InterfaceBuilder<T>.() -> Unit = { derive() }
    ) {
        interfaces += InterfaceBuilder(T::class, name, takeDesc(), this).apply(configure)
    }

    /**
     * Declare [T] as a GraphQL Object type.
     *
     * @param name the name as displayed in the schema. Defaults to the class name.
     * @param configure the DSL to construct this object type. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <reified T : Any> type(
        name: String = T::class.deriveName(),
        configure: TypeBuilder<T>.() -> Unit = { derive() }
    ) {
        types += TypeBuilder(T::class, name, takeDesc(), this).apply(configure)
    }

    /**
     * Use [T] as the root query object.
     *
     * @param configure the DSL to construct this root object. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <T : Any> query(query: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        takeDesc() // Consume description
        this.query = OperationBuilder("Query", query, this).apply(configure)
    }

    /**
     * Create the root query object.
     *
     * @param configure the DSL to construct this root object.
     */
    @SchemaDsl
    inline fun query(configure: OperationBuilder<Any>.() -> Unit) {
        query(object {}, configure)
    }

    /**
     * Use [T] as the root mutation object.
     *
     * @param configure the DSL to construct this root object. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <T : Any> mutation(mutation: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        takeDesc() // Consume description
        this.mutation = OperationBuilder("Mutation", mutation, this).apply(configure)
    }

    /**
     * Create the root mutation object.
     *
     * @param configure the DSL to construct this root object.
     */
    @SchemaDsl
    inline fun mutation(configure: OperationBuilder<Any>.() -> Unit) {
        mutation(object {}, configure)
    }

    /**
     * Use [T] as the root subscription object.
     *
     * @param configure the DSL to construct this root object. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <T : Any> subscription(subscription: T, configure: OperationBuilder<T>.() -> Unit = { derive() }) {
        takeDesc() // Consume description
        this.subscription = OperationBuilder("Subscription", subscription, this).apply(configure)
    }

    /**
     * Create the root subscription object.
     *
     * @param configure the DSL to construct this root object.
     */
    @SchemaDsl
    inline fun subscription(configure: OperationBuilder<Any>.() -> Unit) {
        subscription(object {}, configure)
    }
}
