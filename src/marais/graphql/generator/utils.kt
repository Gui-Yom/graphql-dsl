package marais.graphql.generator

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

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

internal val flowType = Flow::class.createType(listOf(KTypeProjection.STAR))
internal val deferredType = Deferred::class.createType(listOf(KTypeProjection.STAR))
internal val futureType = CompletableFuture::class.createType(listOf(KTypeProjection.STAR))
internal val publisherType = Publisher::class.createType(listOf(KTypeProjection.STAR))

fun KType.isFlow(): Boolean = classifier == flowType.classifier

fun KType.isValidContainer(): Boolean {
    return isFlow() || classifier == deferredType.classifier || classifier == futureType.classifier || classifier == publisherType.classifier
}

fun KType.unwrap(): KType = arguments[0].type!!

fun KType.representationType(): KType = if (isValidContainer()) unwrap() else this

fun KType.name(): String = (classifier!! as KClass<*>).simpleName!!
