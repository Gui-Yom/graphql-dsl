package marais.graphql.dsl

import kotlin.reflect.KClass

fun isValidClassForType(kclass: KClass<*>): Boolean {
    return !(kclass.isSealed || kclass.isAbstract)
}

fun isValidClassForInterface(kclass: KClass<*>): Boolean {
    return kclass.isSealed || kclass.isAbstract || kclass.isOpen
}

/**
 * The functions we do not include automatically while deriving
 */
fun isValidFunctionDerive(name: String): Boolean {
    return name !in listOf("equals", "hashCode", "toString")
}
