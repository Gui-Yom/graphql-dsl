package marais.graphql.dsl

import graphql.schema.StaticDataFetcher
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect
import kotlin.reflect.typeOf

private val log = LoggerFactory.getLogger(SchemaSpec::class.java)

@SchemaDsl
sealed class Type<R : Any>(val name: String) {
    val fields: MutableList<Field> = mutableListOf()

    /**
     * Include a field from a static value. No computations, a plain static value that won't ever change.
     */
    @ExperimentalStdlibApi
    inline fun <reified T : Any> static(name: String, value: T) {
        fields += CustomField(name, null, typeOf<T>(), emptyList(), StaticDataFetcher(value))
    }

    /**
     * Include a field originating from a class property.
     */
    @SchemaDsl
    fun <O> include(
        property: KProperty1<R, O>,
        name: String = property.name,
        description: String? = null
    ) {
        if (fields.containsWithName(name))
            throw Exception("A field with this name is already included")
        fields += PropertyField(property, name, description)
    }

    /**
     * Include a field originating from a class function.
     */
    @SchemaDsl
    fun <O : Any?> include(
        func: KFunction<O>,
        name: String = func.name,
        description: String? = null
    ) {
        if (fields.containsWithName(name))
            throw Exception("A field with this name is already included")
        fields += FunctionField<R>(func, name, description)
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
     */
    fun exclude(func: KFunction<*>) {
        if (!fields.removeIf {
                if (it is FunctionField<*>) {
                    it.func == func
                } else false
            }) throw Exception("Trying to remove a field that's not included")
    }

    /**
     * Exclude all field originating from a class property.
     */
    operator fun KProperty1<R, *>.unaryMinus() {
        exclude(this)
    }

    /**
     * Exclude all field originating from a class function.
     */
    operator fun KFunction<*>.unaryMinus() {
        exclude(this)
    }

    internal fun derive(kclass: KClass<R>, instance: R?) {
        deriveProperties(kclass, instance)
        deriveFunctions(kclass, instance)
    }

    internal fun deriveProperties(kclass: KClass<R>, instance: R?) {
        for (member in kclass.memberProperties) {
            log.debug("found property : $member")
            fields += PropertyField(member, member.name, instance = instance)
        }
    }

    internal fun deriveFunctions(kclass: KClass<R>, instance: R?) {
        for (member in kclass.memberFunctions) {
            if (isValidFunctionDerive(member.name)) {
                log.debug("found function : $member")
                fields += FunctionField(member, member.name, instance = instance)
            }
        }
    }

    //We can't call raw Function<*> because of how kotlin-reflect warks atm, so we have to specify each possibility.

    @SchemaDsl
    fun <O> field(
        name: String,
        description: String? = null,
        resolver: suspend R.() -> O
    ) {
        val reflected = resolver.reflect()!!
        fields += CustomField(
            name,
            description,
            reflected.returnType.representationType(),
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
        description: String? = null,
        resolver: suspend R.(A) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            description,
            reflected.returnType.representationType(),
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
        description: String? = null,
        resolver: suspend R.(A, B) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            description,
            reflected.returnType.representationType(),
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
        description: String? = null,
        resolver: suspend R.(A, B, C) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            description,
            reflected.returnType.representationType(),
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
        description: String? = null,
        resolver: suspend R.(A, B, C, D) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            description,
            reflected.returnType.representationType(),
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
        description: String? = null,
        resolver: suspend R.(A, B, C, D, E) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3]).also { if (!it.isSpecialType()) args += it }
        val arg4 = Argument(reflected.valueParameters[4]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            description,
            reflected.returnType.representationType(),
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
        description: String? = null,
        resolver: suspend R.(A, B, C, D, E, F) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3]).also { if (!it.isSpecialType()) args += it }
        val arg4 = Argument(reflected.valueParameters[4]).also { if (!it.isSpecialType()) args += it }
        val arg5 = Argument(reflected.valueParameters[5]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
            name,
            description,
            reflected.returnType.representationType(),
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

class InterfaceBuilder<R : Any>(val kclass: KClass<R>, name: String? = null) : Type<R>(name ?: kclass.simpleName!!) {

    init {
        require(isValidClassForInterface(kclass))
    }

    /**
     * Include fields based on properties and functions present in the backing class.
     */
    @SchemaDsl
    fun derive() {
        super.derive(kclass, null)
    }
}

class TypeBuilder<R : Any>(val kclass: KClass<R>, name: String?) : Type<R>(name ?: kclass.simpleName!!) {

    init {
        require(isValidClassForType(kclass))
    }

    val interfaces = mutableListOf<KClass<*>>()

    /**
     * Include fields based on properties and functions present in the backing class.
     */
    @SchemaDsl
    fun derive() {
        super.derive(kclass, null)
    }

    /**
     * Declare this type as implementing another interface.
     */
    @SchemaDsl
    inline fun <reified I : Any> inter() {
        // TODO should we check that R : I
        interfaces += I::class
    }
}

class OperationBuilder<R : Any>(name: String, val instance: R) : Type<R>(name) {

    /**
     * Include fields based on properties and functions present in the backing class.
     */
    @SchemaDsl
    fun derive() {
        val kclass = instance::class
        super.derive(kclass as KClass<R>, instance)
    }
}
