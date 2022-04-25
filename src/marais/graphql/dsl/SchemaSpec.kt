package marais.graphql.dsl

import graphql.schema.Coercing
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLScalarType
import org.apache.logging.log4j.Logger
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

@DslMarker
annotation class SchemaDsl

/**
 * Holds the root DSL.
 */
@SchemaDsl
class SchemaSpec internal constructor(log: Logger) : DescriptionDsl {

    val idCoercers = mutableMapOf<KClass<*>, IdCoercer<*>>()
    val scalars = mutableListOf<ScalarSpec>()
    val enums = mutableListOf<EnumSpec>()

    // Maps a kotlin type to a graphql input type declaration
    val inputs = mutableListOf<InputSpec>()

    // Maps a kotlin type to a graphql interface declaration
    val interfaces = mutableListOf<InterfaceSpec<*>>()

    // Maps a kotlin type to a graphql type declaration
    val types = mutableListOf<TypeSpec<*>>()

    lateinit var query: OperationSpec<*>
    var mutation: OperationSpec<*>? = null
    var subscription: OperationSpec<*>? = null

    override val context = SchemaBuilderContext(log, idCoercers, inputs, interfaces)

    /**
     * By default, when generating fetchers for your fields, [kotlinx.coroutines.flow.Flow] will be converted to
     * [org.reactivestreams.Publisher] because the default [graphql.execution.SubscriptionExecutionStrategy] only
     * supports it as subscription return type.
     *
     * Apply this directive at the top of your schema (before any subscription declaration) to prevent this behaviour.
     * This requires you to use an implementation of [graphql.execution.SubscriptionExecutionStrategy] that handle
     * [kotlinx.coroutines.flow.Flow] correctly.
     */
    @SchemaDsl
    fun doNotConvertFlowToPublisher() {
        context.convertFlowToPublisher = false
    }

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
        scalars += ScalarSpec(T::class, name, coercing, context.takeDesc(), builder)
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
        enums += EnumSpec(T::class, name, context.takeDesc(), builder)
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
        inputs += InputSpec(T::class, name, context.takeDesc(), builder)
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
        configure: InterfaceSpec<T>.() -> Unit = { derive() }
    ) {
        interfaces += InterfaceSpec(T::class, name, context).apply(configure)
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
        configure: TypeSpec<T>.() -> Unit = { derive(); deriveInterfaces() }
    ) {
        types += TypeSpec(T::class, name, context).apply(configure)
    }

    /**
     * Use [T] as the root query object.
     *
     * @param configure the DSL to construct this root object. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <T : Any> query(query: T, configure: OperationSpec<T>.() -> Unit = { derive() }) {
        context.takeDesc() // Consume description
        this.query = OperationSpec("Query", query, context).apply(configure)
    }

    /**
     * Create the root query object.
     *
     * @param configure the DSL to construct this root object.
     */
    @SchemaDsl
    inline fun query(configure: OperationSpec<Any>.() -> Unit) {
        query(object {}, configure)
    }

    /**
     * Use [T] as the root mutation object.
     *
     * @param configure the DSL to construct this root object. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <T : Any> mutation(mutation: T, configure: OperationSpec<T>.() -> Unit = { derive() }) {
        context.takeDesc() // Consume description
        this.mutation = OperationSpec("Mutation", mutation, context).apply(configure)
    }

    /**
     * Create the root mutation object.
     *
     * @param configure the DSL to construct this root object.
     */
    @SchemaDsl
    inline fun mutation(configure: OperationSpec<Any>.() -> Unit) {
        mutation(object {}, configure)
    }

    /**
     * Use [T] as the root subscription object.
     *
     * @param configure the DSL to construct this root object. Defaults to including all fields.
     */
    @SchemaDsl
    inline fun <T : Any> subscription(subscription: T, configure: OperationSpec<T>.() -> Unit = { derive() }) {
        context.takeDesc() // Consume description
        this.subscription = OperationSpec("Subscription", subscription, context).apply(configure)
    }

    /**
     * Create the root subscription object.
     *
     * @param configure the DSL to construct this root object.
     */
    @SchemaDsl
    inline fun subscription(configure: OperationSpec<Any>.() -> Unit) {
        subscription(object {}, configure)
    }
}
