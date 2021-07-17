package marais.graphql.dsl

import graphql.schema.StaticDataFetcher
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect

@SchemaDsl
sealed class BaseTypeBuilder<R : Any>(
    val kclass: KClass<R>,
    val instance: R? = null,
    val name: String = kclass.simpleName!!,
    val description: String? = null,
    val idCoercers: Map<KClass<*>, IdCoercer<*>> = emptyMap()
) : DescriptionPublisher {
    val fields: MutableList<Field> = mutableListOf()

    // For the DescriptionPublisher implementation
    override var nextDesc: String? = null

    /**
     * Include a field from a static value. No computations, a plain static value that won't ever change.
     */
    inline fun <reified T : Any> static(name: String, value: T) {
        // TODO Use typeOf() here when stabilized
        fields += CustomField(name, null, T::class.starProjectedType, emptyList(), StaticDataFetcher(value))
    }

    /**
     * Include a field originating from a class property.
     */
    @SchemaDsl
    fun <O> include(
        property: KProperty1<R, O>,
        name: String = property.name
    ) {
        if (name in fields)
            throw Exception("A field with this name is already included")
        fields += PropertyField(property, name, takeDesc())
    }

    /**
     * Include a field originating from a class function.
     */
    @SchemaDsl
    fun <O : Any?> include(
        func: KFunction<O>,
        name: String = func.name,
    ) {
        if (name in fields)
            throw Exception("A field with this name is already included")
        fields += FunctionField<R>(func, name, takeDesc(), null, idCoercers)
    }

    /**
     * Include a field originating from a class property.
     */
    operator fun KProperty1<R, *>.unaryPlus() {
        include(this)
    }

    /**
     * Include a field originating from a class function.
     */
    operator fun KFunction<*>.unaryPlus() {
        include(this)
    }

    fun derive(
        kclass: KClass<R>,
        nameFilter: List<String>,
        propFilter: List<KProperty1<R, *>>,
        funFilter: List<KFunction<*>>,
    ) {
        deriveProperties(kclass, nameFilter, propFilter)
        deriveFunctions(kclass, nameFilter, funFilter)
        // TODO handle the case where a property and a function with the same name exist
    }

    fun deriveProperties(
        kclass: KClass<R>,
        nameFilter: List<String>,
        propFilter: List<KProperty1<R, *>>,
    ) {
        kclass.memberProperties.asSequence().filter {
            it.name !in nameFilter
        }.filter {
            it !in propFilter
        }.forEach {
            log.debug("[derive] ${name}[${kclass.qualifiedName}] property `${it.name}`: ${it.returnType}")
            fields += PropertyField(it, it.name, null, instance)
        }
    }

    fun deriveFunctions(
        kclass: KClass<R>,
        nameFilter: List<String>,
        funFilter: List<KFunction<*>>,
    ) {
        kclass.memberFunctions.asSequence().filter {
            it.name.isValidFunctionForDerive() && it.name !in nameFilter
        }.filter {
            it !in funFilter
        }.forEach {
            log.debug("[derive] ${name}[${kclass.qualifiedName}] function `${it.name}`: ${it.returnType}")
            fields += FunctionField(it, it.name, null, instance, idCoercers)
        }
    }

    /**
     * Include fields based on properties and functions present in the backing class.
     */
    @SchemaDsl
    inline fun derive(exclusionsBuilder: ExclusionFilterBuilder<R>.() -> Unit = {}) {
        val exclusions = ExclusionFilterBuilder<R>().apply(exclusionsBuilder)
        derive(kclass, exclusions.nameExclusions, exclusions.propExclusions, exclusions.funExclusions)
    }

    @SchemaDsl
    inline fun deriveProperties(exclusionsBuilder: PropertyExclusionFilterBuilder<R>.() -> Unit = {}) {
        val exclusions = PropertyExclusionFilterBuilder<R>().apply(exclusionsBuilder)
        deriveProperties(kclass, exclusions.nameExclusions, exclusions.propExclusions)
    }

    @SchemaDsl
    inline fun deriveFunctions(exclusionsBuilder: FunctionExclusionFilterBuilder.() -> Unit = {}) {
        val exclusions = FunctionExclusionFilterBuilder().apply(exclusionsBuilder)
        deriveFunctions(kclass, exclusions.nameExclusions, exclusions.funExclusions)
    }

    //We can't call raw Function<*> because of how kotlin-reflect warks atm, so we have to specify each possibility.
    // TODO explore the possibility of making these functions inline to remove the call to resolver.reflect()

    @SchemaDsl
    fun <O> field(
        name: String,
        resolver: suspend R.() -> O
    ) {
        val reflected = resolver.reflect()!!
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            emptyList(),
            suspendFetcher {
                resolver(
                    instance ?: it.getSource()
                )
            })
    }

    // TODO Maybe inlining could be better to obtain the type of A
    // We need to reflect the lambda anyway to get param names
    @SchemaDsl
    fun <O, A> field(
        name: String,
        resolver: suspend R.(A) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 =
            reflected.valueParameters[0].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    instance ?: it.getSource(),
                    arg0.resolve(it)
                )
            })
    }

    @SchemaDsl
    fun <O, A, B> field(
        name: String,
        resolver: suspend R.(A, B) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 =
            reflected.valueParameters[0].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg1 =
            reflected.valueParameters[1].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    instance ?: it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it)
                )
            })
    }

    @SchemaDsl
    fun <O, A, B, C> field(
        name: String,
        resolver: suspend R.(A, B, C) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 =
            reflected.valueParameters[0].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg1 =
            reflected.valueParameters[1].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg2 =
            reflected.valueParameters[2].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    instance ?: it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it)
                )
            })
    }

    @SchemaDsl
    fun <O, A, B, C, D> field(
        name: String,
        resolver: suspend R.(A, B, C, D) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 =
            reflected.valueParameters[0].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg1 =
            reflected.valueParameters[1].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg2 =
            reflected.valueParameters[2].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg3 =
            reflected.valueParameters[3].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    instance ?: it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it),
                    arg3.resolve(it)
                )
            })
    }

    @SchemaDsl
    fun <O, A, B, C, D, E> field(
        name: String,
        resolver: suspend R.(A, B, C, D, E) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 =
            reflected.valueParameters[0].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg1 =
            reflected.valueParameters[1].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg2 =
            reflected.valueParameters[2].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg3 =
            reflected.valueParameters[3].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg4 =
            reflected.valueParameters[4].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    instance ?: it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it),
                    arg3.resolve(it),
                    arg4.resolve(it)
                )
            })
    }

    @SchemaDsl
    fun <O, A, B, C, D, E, F> field(
        name: String,
        resolver: suspend R.(A, B, C, D, E, F) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 =
            reflected.valueParameters[0].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg1 =
            reflected.valueParameters[1].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg2 =
            reflected.valueParameters[2].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg3 =
            reflected.valueParameters[3].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg4 =
            reflected.valueParameters[4].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        val arg5 =
            reflected.valueParameters[5].createArgument(idCoercers).also { if (it !is EnvArgument) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    instance ?: it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it),
                    arg3.resolve(it),
                    arg4.resolve(it),
                    arg5.resolve(it)
                )
            })
    }
}

class InterfaceBuilder<R : Any>(
    kclass: KClass<R>,
    name: String? = null,
    description: String? = null,
    idCoercers: Map<KClass<*>, IdCoercer<*>>
) : BaseTypeBuilder<R>(kclass, null, name ?: kclass.simpleName!!, description, idCoercers) {

    init {
        require(kclass.isValidClassForInterface())
    }
}

class TypeBuilder<R : Any>(
    kclass: KClass<R>,
    name: String?,
    description: String? = null,
    idCoercers: Map<KClass<*>, IdCoercer<*>>
) : BaseTypeBuilder<R>(kclass, null, name ?: kclass.simpleName!!, description, idCoercers) {

    init {
        require(kclass.isValidClassForType())
    }

    /**
     * Interfaces implemented by this type
     */
    val interfaces = mutableListOf<KClass<*>>()

    /**
     * Declare this type as implementing another interface.
     */
    @SchemaDsl
    inline fun <reified I : Any> inter() {
        // TODO we should check that R : I
        interfaces += I::class
    }
}

class OperationBuilder<R : Any>(name: String, instance: R, inputCoercers: Map<KClass<*>, IdCoercer<*>>) :
    BaseTypeBuilder<R>(instance::class as KClass<R>, instance, name, idCoercers = inputCoercers) {

    init {
        require(kclass.isValidClassForType())
    }
}
