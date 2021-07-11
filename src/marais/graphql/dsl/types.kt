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
    val name: String = kclass.simpleName!!,
    val description: String? = null,
    val inputCoercers: Map<KClass<*>, IdConverter<*>>
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
        fields += FunctionField<R>(func, name, takeDesc(), inputCoercers = inputCoercers)
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

    /**
     * Exclude all field originating from a class property.
     *
     * @throws Exception if this property wasn't included
     */
    fun exclude(prop: KProperty1<R, *>) {
        if (!fields.removeIf {
                if (it is PropertyField<*>) {
                    it.property == prop
                } else false
            }) throw Exception("Trying to remove a field that's not included")
    }

    /**
     * Exclude all field originating from a class function.
     *
     * @throws Exception if this function wasn't included
     */
    fun exclude(func: KFunction<*>) {
        if (!fields.removeIf {
                if (it is FunctionField<*>) {
                    it.func == func
                } else false
            }) throw Exception("Trying to remove a field that's not included")
    }

    /**
     * Exclude all fields with the given name.
     *
     * @throws Exception if no field exists with this name
     */
    fun exclude(name: String) {
        if (!fields.removeIf {
                if (it is FunctionField<*>) {
                    it.name == name
                } else false
            }) throw Exception("Trying to remove a field that's not included")
    }

    /**
     * Exclude all field originating from a class property.
     *
     * @throws Exception if this property wasn't included
     */
    operator fun KProperty1<R, *>.unaryMinus() {
        exclude(this)
    }

    /**
     * Exclude all field originating from a class function.
     *
     * @throws Exception if this function wasn't included
     */
    operator fun KFunction<*>.unaryMinus() {
        exclude(this)
    }

    /**
     * Exclude all fields with the given name.
     *
     * @throws Exception if no field exists with this name
     */
    operator fun String.unaryMinus() {
        exclude(this)
    }

    internal fun derive(kclass: KClass<R>, instance: R?) {
        deriveProperties(kclass, instance)
        deriveFunctions(kclass, instance)
        // FIXME handle the case where a property and a function with the same name exist
    }

    internal fun deriveProperties(kclass: KClass<R>, instance: R?) {
        for (member in kclass.memberProperties) {
            log.debug("[derive] ${name}[${kclass.qualifiedName}] property ${member.name}: ${member.returnType}")
            fields += PropertyField(member, member.name, instance = instance)
        }
    }

    internal fun deriveFunctions(kclass: KClass<R>, instance: R?) {
        for (member in kclass.memberFunctions) {
            if (member.name.isValidFunctionForDerive()) {
                log.debug("[derive] ${name}[${kclass.qualifiedName}] function ${member.name}: ${member.returnType}")
                fields += FunctionField(member, member.name, instance = instance, inputCoercers = inputCoercers)
            }
        }
    }

    /**
     * Include fields based on properties and functions present in the backing class.
     */
    @SchemaDsl
    open fun derive() {
        derive(kclass, null)
    }

    @SchemaDsl
    fun deriveProperties() {
        deriveProperties(kclass, null)
    }

    @SchemaDsl
    fun deriveFunctions() {
        deriveFunctions(kclass, null)
    }

    //We can't call raw Function<*> because of how kotlin-reflect warks atm, so we have to specify each possibility.

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
                    it.getSource()
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
        val arg0 = Argument(reflected.valueParameters[0], inputCoercers).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    it.getSource(),
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
        val arg0 = Argument(reflected.valueParameters[0], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1], inputCoercers).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    it.getSource(),
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
        val arg0 = Argument(reflected.valueParameters[0], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2], inputCoercers).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    it.getSource(),
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
        val arg0 = Argument(reflected.valueParameters[0], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3], inputCoercers).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    it.getSource(),
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
        val arg0 = Argument(reflected.valueParameters[0], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg4 = Argument(reflected.valueParameters[4], inputCoercers).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    it.getSource(),
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
        val arg0 = Argument(reflected.valueParameters[0], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg4 = Argument(reflected.valueParameters[4], inputCoercers).also { if (!it.isSpecialType()) args += it }
        val arg5 = Argument(reflected.valueParameters[5], inputCoercers).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            takeDesc(),
            reflected.returnType.unwrapAsyncType(),
            args,
            suspendFetcher {
                resolver(
                    it.getSource(),
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
    inputCoercers: Map<KClass<*>, IdConverter<*>>
) : BaseTypeBuilder<R>(kclass, name ?: kclass.simpleName!!, description, inputCoercers) {

    init {
        require(kclass.isValidClassForInterface())
    }
}

class TypeBuilder<R : Any>(
    kclass: KClass<R>,
    name: String?,
    description: String? = null,
    inputCoercers: Map<KClass<*>, IdConverter<*>>
) : BaseTypeBuilder<R>(kclass, name ?: kclass.simpleName!!, description, inputCoercers) {

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

class OperationBuilder<R : Any>(name: String, val instance: R, inputCoercers: Map<KClass<*>, IdConverter<*>>) :
    BaseTypeBuilder<R>(instance::class as KClass<R>, name, inputCoercers = inputCoercers) {

    init {
        require(kclass.isValidClassForType())
    }

    /**
     * Include fields based on properties and functions present in the backing class.
     */
    @SchemaDsl
    override fun derive() {
        super.derive(kclass, instance)
    }
}
