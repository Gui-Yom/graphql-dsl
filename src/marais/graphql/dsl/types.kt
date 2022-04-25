package marais.graphql.dsl

import graphql.schema.StaticDataFetcher
import kotlin.reflect.*
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

/**
 * Base builder for every object type
 */
@SchemaDsl
sealed class BaseTypeSpec<R : Any>(
    val kclass: KClass<R>,
    @PublishedApi
    internal val instance: R?,
    val name: String,
    final override val context: SchemaBuilderContext
) : DescriptionDsl {
    val description: String? = context.takeDesc() ?: kclass.extractDesc()
    val fields: MutableList<FieldSpec> = mutableListOf()

    /**
     * Include a field from a static value. No computations, a plain static value that won't ever change.
     *
     * @param name the name of this field.
     * @param value the value returned by this field.
     * @param T the type of this field as shown in the schema. By default, it is inferred from [value].
     */
    @SchemaDsl
    inline fun <reified T : Any> static(name: String, value: T) {
        fields += CustomFieldSpec(name, context.takeDesc(), typeOf<T>(), emptyList(), StaticDataFetcher(value))
    }

    /**
     * Include a field originating from a class property.
     *
     * @param property the property to include as a field.
     * @param name the name of the field, defaults to the name of the property.
     * @param O the type of the field as shown in the schema. By default, it is inferred from the type of [property].
     */
    @SchemaDsl
    fun <O> include(
        property: KProperty1<R, O>,
        name: String = property.name
    ) {
        if (name in fields)
            throw Exception("A field with this name is already included")
        fields += PropertyFieldSpec(property, name, instance, context)
    }

    /**
     * Include a field originating from a class property.
     */
    operator fun KProperty1<R, *>.unaryPlus() {
        include(this)
    }

    /**
     * Include a field originating from a class function.
     *
     * @param func the function to include as a field.
     * @param name the name of the field, defaults to the name of the function.
     * @param O the type of the field as shown in the schema. By default, it is inferred from the type of [func].
     */
    @SchemaDsl
    fun <O : Any?> include(
        func: KFunction<O>,
        name: String = func.name,
    ) {
        if (name in fields)
            throw Exception("A field with this name is already included")
        fields += FunctionFieldSpec(func, name, instance, context)
    }

    /**
     * Include a field originating from a class function.
     */
    operator fun KFunction<*>.unaryPlus() {
        include(this)
    }

    fun derive(
        nameFilter: List<String>,
        propFilter: List<KProperty1<R, *>>,
        funFilter: List<KFunction<*>>,
    ) {
        deriveProperties(nameFilter, propFilter)
        deriveFunctions(nameFilter, funFilter)
        // TODO handle the case where a property and a function with the same name exist
    }

    fun deriveProperties(
        nameFilter: List<String>,
        propFilter: List<KProperty1<R, *>>,
    ) {
        kclass.memberProperties.asSequence().filter {
            it.name !in nameFilter && it !in propFilter && it.visibility == KVisibility.PUBLIC
        }.forEach {
            context.logDerive.debug("${name}[${kclass.qualifiedName}] property `${it.name}`: ${it.returnType}")
            fields += PropertyFieldSpec(it, it.name, instance, context)
        }
    }

    fun deriveFunctions(
        nameFilter: List<String>,
        funFilter: List<KFunction<*>>,
    ) {
        kclass.memberFunctions.asSequence().filter {
            it.name.isValidFunctionForDerive() && it.name !in nameFilter && it !in funFilter && it.visibility == KVisibility.PUBLIC
        }.forEach {
            context.logDerive.debug("${name}[${kclass.qualifiedName}] function `${it.name}`: ${it.returnType}")
            fields += FunctionFieldSpec(it, it.name, instance, context)
        }
    }

    /**
     * Include fields based on public properties and functions present in the backing class.
     *
     * @param exclusionsBuilder allows you to configure exclusion rules, defaults to a set of known functions.
     */
    @SchemaDsl
    inline fun derive(exclusionsBuilder: ExclusionFilterBuilder<R>.() -> Unit = {}) {
        val exclusions = ExclusionFilterBuilder<R>().apply(exclusionsBuilder)
        derive(exclusions.nameExclusions, exclusions.propExclusions, exclusions.funExclusions)
    }

    /**
     * Include fields based on public properties present in the backing class.
     *
     * @param exclusionsBuilder allows you to configure exclusion rules, defaults to no exclusions.
     */
    @SchemaDsl
    inline fun deriveProperties(exclusionsBuilder: PropertyExclusionFilterBuilder<R>.() -> Unit = {}) {
        val exclusions = PropertyExclusionFilterBuilder<R>().apply(exclusionsBuilder)
        deriveProperties(exclusions.nameExclusions, exclusions.propExclusions)
    }

    /**
     * Include fields based on public functions present in the backing class.
     *
     * @param exclusionsBuilder allows you to configure exclusion rules, defaults to a set of known functions.
     */
    @SchemaDsl
    inline fun deriveFunctions(exclusionsBuilder: FunctionExclusionFilterBuilder.() -> Unit = {}) {
        val exclusions = FunctionExclusionFilterBuilder().apply(exclusionsBuilder)
        deriveFunctions(exclusions.nameExclusions, exclusions.funExclusions)
    }

    // We can't call raw Function<*> because of how kotlin-reflect works atm, so we have to specify each possibility.
    // We pass a reference to a method that call our lambda for us, so we get a standard KFunction for our fetcher
    // We then pass the lambda as a hidden argument to our field
    // Tbh, I kinda feel like a genius for coming up with this

    // For that first field() impl, the lambda don't have any parameters, so we can use an inline function
    // This special impl is kept for demonstration, we could have used the general impl
    // Sadly, we still need to call .reflect() on other lambdas to obtain the parameter names

    /**
     * Declare a custom field.
     *
     * @param name the name of the field
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    fun <O> field(
        name: String,
        fetcher: suspend R.() -> O
    ) {
        fields += SuspendLambdaFieldSpec(name, fetcher, 0, context, instance)
    }

    /**
     * Declare a custom field.
     *
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    operator fun <O> String.invoke(fetcher: suspend R.() -> O) {
        field(this, fetcher)
    }

    /**
     * Declare a custom field.
     *
     * @param name the name of the field
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    fun <O, A> field(
        name: String,
        fetcher: suspend R.(A) -> O
    ) {
        fields += SuspendLambdaFieldSpec(name, fetcher, 1, context, instance)
    }

    /**
     * Declare a custom field.
     *
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    operator fun <O, A> String.invoke(fetcher: suspend R.(A) -> O) {
        field(this, fetcher)
    }

    /**
     * Declare a custom field.
     *
     * @param name the name of the field
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    fun <O, A, B> field(
        name: String,
        fetcher: suspend R.(A, B) -> O
    ) {
        fields += SuspendLambdaFieldSpec(name, fetcher, 2, context, instance)
    }

    /**
     * Declare a custom field.
     *
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    operator fun <O, A, B> String.invoke(fetcher: suspend R.(A, B) -> O) {
        field(this, fetcher)
    }

    /**
     * Declare a custom field.
     *
     * @param name the name of the field
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    fun <O, A, B, C> field(
        name: String,
        fetcher: suspend R.(A, B, C) -> O
    ) {
        fields += SuspendLambdaFieldSpec(name, fetcher, 3, context, instance)
    }

    /**
     * Declare a custom field.
     *
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    operator fun <O, A, B, C> String.invoke(fetcher: suspend R.(A, B, C) -> O) {
        field(this, fetcher)
    }

    /**
     * Declare a custom field.
     *
     * @param name the name of the field
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    fun <O, A, B, C, D> field(
        name: String,
        fetcher: suspend R.(A, B, C, D) -> O
    ) {
        fields += SuspendLambdaFieldSpec(name, fetcher, 4, context, instance)
    }

    /**
     * Declare a custom field.
     *
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    operator fun <O, A, B, C, D> String.invoke(fetcher: suspend R.(A, B, C, D) -> O) {
        field(this, fetcher)
    }

    /**
     * Declare a custom field.
     *
     * @param name the name of the field
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    fun <O, A, B, C, D, E> field(
        name: String,
        fetcher: suspend R.(A, B, C, D, E) -> O
    ) {
        fields += SuspendLambdaFieldSpec(name, fetcher, 5, context, instance)
    }

    /**
     * Declare a custom field.
     *
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    operator fun <O, A, B, C, D, E> String.invoke(fetcher: suspend R.(A, B, C, D, E) -> O) {
        field(this, fetcher)
    }

    /**
     * Declare a custom field.
     *
     * @param name the name of the field
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    fun <O, A, B, C, D, E, F> field(
        name: String,
        fetcher: suspend R.(A, B, C, D, E, F) -> O
    ) {
        fields += SuspendLambdaFieldSpec(name, fetcher, 6, context, instance)
    }

    /**
     * Declare a custom field.
     *
     * @param fetcher the code executed behind this field
     */
    @SchemaDsl
    operator fun <O, A, B, C, D, E, F> String.invoke(fetcher: suspend R.(A, B, C, D, E, F) -> O) {
        field(this, fetcher)
    }
}

/**
 * DSL for building a GraphQL interface.
 */
class InterfaceSpec<R : Any>(
    kclass: KClass<R>,
    name: String,
    context: SchemaBuilderContext
) : BaseTypeSpec<R>(kclass, null, name, context) {

    init {
        require(kclass.isValidClassForInterface())
    }
}

/**
 * DSL for building a GraphQL Object type.
 */
class TypeSpec<R : Any>(
    kclass: KClass<R>,
    name: String,
    context: SchemaBuilderContext
) : BaseTypeSpec<R>(kclass, null, name, context) {

    init {
        require(kclass.isValidClassForType())
    }

    /**
     * Interfaces implemented by this type
     */
    val interfaces = mutableListOf<KClass<*>>()

    /**
     * Declare this type as implementing interface [I].
     *
     * @param I the interface this type should be implementing
     */
    @SchemaDsl
    inline fun <reified I : Any> inter() {
        // TODO check that R : I
        interfaces += I::class
        // TODO include interface fields
    }

    /**
     * Automatically implement known interfaces.
     */
    @SchemaDsl
    fun deriveInterfaces() {
        interfaces += context.getImplementedInterfaces(kclass)
    }
}

/**
 * DSL for building a root object.
 */
class OperationSpec<R : Any>(name: String, instance: R, context: SchemaBuilderContext) :
    BaseTypeSpec<R>(instance::class as KClass<R>, instance, name, context) {

    init {
        require(kclass.isValidClassForType())
    }
}
