package marais.graphql.dsl

import graphql.schema.StaticDataFetcher
import marais.graphql.generator.*
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect
import kotlin.reflect.typeOf

private val log = LoggerFactory.getLogger(SchemaBuilder::class.java)

@SchemaDsl
sealed class Type<R : Any>(val name: String) {
    val fields: MutableList<Field> = mutableListOf()

    @ExperimentalStdlibApi
    inline fun <reified T : Any> static(name: String, value: T) {
        fields += CustomField(name, null, typeOf<T>(), emptyList(), StaticDataFetcher(value))
    }

    @SchemaDsl
    fun <O> field(
        property: KProperty1<R, O>,
        name: String? = null,
        description: String? = null
    ) {
        fields += PropertyField(property, name, description)
    }

    @SchemaDsl
    fun <O : Any?> field(
        func: KFunction<O>,
        name: String? = null,
        description: String? = null
    ) {
        fields += FunctionField<R>(func, name, description)
    }

    operator fun KProperty1<R, *>.unaryMinus() {
        exclude(this)
    }

    operator fun KFunction<*>.unaryMinus() {
        exclude(this)
    }

    fun exclude(prop: KProperty1<R, *>) {
        fields.removeIf {
            if (it is PropertyField<*>) {
                it.property == prop
            } else false
        }
    }

    fun exclude(func: KFunction<*>) {
        fields.removeIf {
            if (it is FunctionField<*>) {
                it.func == func
            } else false
        }
    }

    internal fun derive(kclass: KClass<R>, instance: R?) {
        deriveProperties(kclass, instance)
        deriveFunctions(kclass, instance)
    }

    internal fun deriveProperties(kclass: KClass<R>, instance: R?) {
        for (member in kclass.memberProperties) {
            log.debug("found property : $member")
            fields += PropertyField(member, instance = instance)
        }
    }

    internal fun deriveFunctions(kclass: KClass<R>, instance: R?) {
        for (member in kclass.memberFunctions) {
            if (isValidFunctionDerive(member.name)) {
                log.debug("found function : $member")
                fields += FunctionField(member, instance = instance)
            }
        }
    }
}

class InterfaceBuilder<R : Any>(val kclass: KClass<R>, name: String? = null) : Type<R>(name ?: kclass.simpleName!!) {

    @SchemaDsl
    fun derive() {
        require(isValidClassForInterface(kclass))

        super.derive(kclass, null)
    }
}

class TypeBuilder<R : Any>(val kclass: KClass<R>, name: String?) : Type<R>(name ?: kclass.simpleName!!) {

    val interfaces = mutableListOf<KClass<*>>()

    @SchemaDsl
    fun derive() {

        require(isValidClassForType(kclass))

        super.derive(kclass, null)
    }

    @SchemaDsl
    inline fun <reified I : Any> inter() {
        // TODO check R : I
        interfaces += I::class
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

class OperationBuilder<R : Any>(name: String, val instance: R) : Type<R>(name) {

    @SchemaDsl
    fun derive() {

        val kclass = instance::class

        require(isValidClassForType(kclass))

        super.derive(kclass as KClass<R>, instance)
    }
}
