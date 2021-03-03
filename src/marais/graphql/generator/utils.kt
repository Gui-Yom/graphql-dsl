package marais.graphql.generator

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

internal fun isValidClassForType(kclass: KClass<*>): Boolean {
    return !(kclass.isSealed || kclass.isAbstract)
}

internal fun isValidClassForInterface(kclass: KClass<*>): Boolean {
    return kclass.isSealed || kclass.isAbstract || kclass.isOpen
}

internal val invalidFunctionName = listOf("equals", "hashCode", "toString", "copy")
internal val componentPattern = Regex("component[0-9]+")

/**
 * The functions we do not include automatically while deriving
 */
internal fun isValidFunctionDerive(name: String): Boolean {
    return name !in invalidFunctionName && !componentPattern.matches(name)
}

internal val flowType = Flow::class.createType(listOf(KTypeProjection.STAR))
internal val deferredType = Deferred::class.createType(listOf(KTypeProjection.STAR))
internal val futureType = CompletableFuture::class.createType(listOf(KTypeProjection.STAR))
internal val publisherType = Publisher::class.createType(listOf(KTypeProjection.STAR))

internal fun KType.isFlow(): Boolean = classifier == flowType.classifier

internal fun KType.isValidContainer(): Boolean {
    return isFlow() || classifier == deferredType.classifier || classifier == futureType.classifier || classifier == publisherType.classifier
}

internal fun KType.unwrap(): KType = arguments[0].type!!

internal fun KType.representationType(): KType = if (isValidContainer()) unwrap() else this

internal fun KType.name(): String = (classifier!! as KClass<*>).simpleName!!
