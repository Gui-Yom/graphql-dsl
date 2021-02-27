package marais.graphql.dsl

import graphql.schema.StaticDataFetcher
import marais.graphql.generator.*
import kotlin.reflect.*
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect

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
        fields.removeIf {
            if (it is PropertyField<*>) {
                it.property == this
            } else false
        }
    }

    operator fun KFunction<*>.unaryMinus() {
        fields.removeIf {
            if (it is FunctionField<*>) {
                it.func == this
            } else false
        }
    }

    internal fun derive(kclass: KClass<R>, instance: R?) {
        for (member in kclass.members) {
            when (member) {
                is KProperty1<*, *> -> {
                    println("found property : $member")
                    fields += PropertyField(member as KProperty1<R, *>, instance = instance)
                }
                is KFunction<*> -> {
                    if (isValidFunctionDerive(member.name)) {
                        println("found function : $member")
                        fields += FunctionField(member, instance = instance)
                    }
                }
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

    val interfaces = mutableListOf<KType>()

    @SchemaDsl
    fun derive() {

        require(isValidClassForType(kclass))

        super.derive(kclass, null)
    }

    @ExperimentalStdlibApi
    @SchemaDsl
    inline fun <reified I : Any> inter() {
        val ktype = typeOf<I>()
        interfaces += ktype
    }

    //We can't call raw Function<*> because of how kotlin-reflect warks atm, so we have to specify each possibility.

    // TODO Maybe inlining could be better to obtain the type of A
    // We need to reflect the lambda anyway to get param names
    @SchemaDsl
    fun <O, A> field(
            name: String,
            description: String? = null,
            resolver: R.(A) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
                name,
                description,
                if (reflected.returnType.isValidContainer()) reflected.returnType.unwrap() else reflected.returnType,
                args
        ) {
            resolver(
                    it.getSource(),
                    arg0.resolve(it)
            )
        }
    }

    @SchemaDsl
    fun <O, A, B> field(
            name: String,
            description: String? = null,
            resolver: R.(A, B) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(
                name,
                description,
                if (reflected.returnType.isValidContainer()) reflected.returnType.unwrap() else reflected.returnType,
                args
        ) {
            resolver(
                    it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it)
            )
        }
    }

    @SchemaDsl
    fun <O, A, B, C> field(
            name: String,
            description: String? = null,
            resolver: R.(A, B, C) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(name, description, reflected.returnType, args) {
            resolver(
                    it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it)
            )
        }
    }

    @SchemaDsl
    fun <O, A, B, C, D> field(
            name: String,
            description: String? = null,
            resolver: R.(A, B, C, D) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(name, description, reflected.returnType, args) {
            resolver(
                    it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it),
                    arg3.resolve(it)
            )
        }
    }

    @SchemaDsl
    fun <O, A, B, C, D, E> field(
            name: String,
            description: String? = null,
            resolver: R.(A, B, C, D, E) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3]).also { if (!it.isSpecialType()) args += it }
        val arg4 = Argument(reflected.valueParameters[4]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(name, description, reflected.returnType, args) {
            resolver(
                    it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it),
                    arg3.resolve(it),
                    arg4.resolve(it)
            )
        }
    }

    @SchemaDsl
    fun <O, A, B, C, D, E, F> field(
            name: String,
            description: String? = null,
            resolver: R.(A, B, C, D, E, F) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        val arg2 = Argument(reflected.valueParameters[2]).also { if (!it.isSpecialType()) args += it }
        val arg3 = Argument(reflected.valueParameters[3]).also { if (!it.isSpecialType()) args += it }
        val arg4 = Argument(reflected.valueParameters[4]).also { if (!it.isSpecialType()) args += it }
        val arg5 = Argument(reflected.valueParameters[5]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(name, description, reflected.returnType, args) {
            resolver(
                    it.getSource(),
                    arg0.resolve(it),
                    arg1.resolve(it),
                    arg2.resolve(it),
                    arg3.resolve(it),
                    arg4.resolve(it),
                    arg5.resolve(it)
            )
        }
    }
}

class OperationBuilder<R : Any>(name: String, val instance: R) : Type<R>(name) {

    @SchemaDsl
    fun derive() {

        val kclass = instance!!::class

        require(isValidClassForType(kclass))

        super.derive(kclass as KClass<R>, instance)
    }
}
