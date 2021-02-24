package marais.graphql.dsl

import kotlin.reflect.*
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.reflect

abstract class Type<R>(val name: String) {
    val fields: MutableList<Field<*>> = mutableListOf()
}

@SchemaDsl
class InterfaceBuilder<R : Any>(val kclass: KClass<R>, name: String? = null) : Type<R>(name ?: kclass.simpleName!!) {

    fun derive() {
        if (!kclass.isSealed && !kclass.isAbstract && !kclass.isOpen) {
            throw Exception("Can't derive $kclass as an interface")
        }

        for (member in kclass.members) {
            when (member) {
                is KProperty1<*, *> -> {
                    println("found property : $member")
                    field(member as KProperty1<R, *>)
                }
                is KFunction<*> -> {
                    if (member.name !in listOf("equals", "hashCode", "toString")) {
                        println("found function : $member")
                        field(member)
                    }
                }
            }
        }
    }

    fun <O> field(
        property: KProperty1<R, O>,
        name: String? = null,
        description: String? = null
    ) {
        fields += PropertyField(property, name, description)
    }

    fun <O> field(
        func: KFunction<O>,
        name: String? = null,
        description: String? = null
    ) {
        fields += FunctionField<R, O>(func, name, description)
    }

    operator fun KProperty1<R, *>.unaryMinus() {
        fields.removeIf {
            if (it is PropertyField<*, *>) {
                it.property == this
            } else false
        }
    }

    operator fun KFunction<*>.unaryMinus() {
        fields.removeIf {
            if (it is FunctionField<*, *>) {
                it.func == this
            } else false
        }
    }
}

@SchemaDsl
class TypeBuilder<R : Any>(val kclass: KClass<R>, name: String?) : Type<R>(name ?: kclass.simpleName!!) {

    val interfaces = mutableListOf<KType>()

    fun derive() {

        if (kclass.isSealed || kclass.isAbstract) {
            throw Exception("Can't derive $kclass as a type")
        }

        for (member in kclass.members) {
            when (member) {
                is KProperty1<*, *> -> {
                    println("found property : $member")
                    field(member as KProperty1<R, *>)
                }
                is KFunction<*> -> {
                    if (member.name !in listOf("equals", "hashCode", "toString")) {
                        println("found function : $member")
                        field(member)
                    }
                }
            }
        }
    }

    @ExperimentalStdlibApi
    inline fun <reified I : Any> inter() {
        val ktype = typeOf<I>()
        interfaces += ktype
    }

    fun <O> field(
        property: KProperty1<R, O>,
        name: String? = null,
        description: String? = null
    ) {
        fields += PropertyField(property, name, description)
    }

    fun <O> field(
        func: KFunction<O>,
        name: String? = null,
        description: String? = null
    ) {
        fields += FunctionField<R, O>(func, name, description)
    }

    operator fun KProperty1<R, *>.unaryMinus() {
        fields.removeIf {
            if (it is PropertyField<*, *>) {
                it.property == this
            } else false
        }
    }

    operator fun KFunction<*>.unaryMinus() {
        fields.removeIf {
            if (it is FunctionField<*, *>) {
                it.func == this
            } else false
        }
    }

    // TODO Maybe inlining could be better to obtain the type of A
    // We need to reflect the lambda anyway
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
            reflected.returnType,
            args
        ) {
            resolver(
                it.getSource(),
                arg0.resolve(it)
            )
        }
    }

    fun <O, A, B> field(
        name: String,
        description: String? = null,
        resolver: R.(A, B) -> O
    ) {
        val reflected = resolver.reflect()!!
        val args = mutableListOf<Argument>()
        val arg0 = Argument(reflected.valueParameters[0]).also { if (!it.isSpecialType()) args += it }
        val arg1 = Argument(reflected.valueParameters[1]).also { if (!it.isSpecialType()) args += it }
        fields += CustomField(name, description, reflected.returnType, args) {
            resolver(
                it.getSource(),
                arg0.resolve(it),
                arg1.resolve(it)
            )
        }
    }

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
}
