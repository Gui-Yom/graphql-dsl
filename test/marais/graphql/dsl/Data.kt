package marais.graphql.dsl

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.net.URL
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

abstract class Node(open val id: MyId)

class Foo(id: MyId, val field: Int) : Node(id) {
    fun dec(): Int = field - 1
}

data class Bar(override val id: MyId, val field: URL) : Node(id) {

    fun additional(param: String): String = param
}

object UrlCoercing : Coercing<URL, String> {
    override fun serialize(dataFetcherResult: Any): String {
        return dataFetcherResult.toString()
    }

    override fun parseValue(input: Any): URL =
        if (input is StringValue) try {
            URL(input.value)
        } catch (e: Exception) {
            throw CoercingParseValueException(e)
        } else throw CoercingParseValueException("Expected a StringValue for Url")

    override fun parseLiteral(input: Any): URL = try {
        URL(input as String)
    } catch (e: Exception) {
        throw CoercingParseLiteralException(e)
    }
}

enum class Baz {
    VALUE0,
    VALUE1,
    VALUE2
}

data class Input(val a: String)

data class MyId(val inner: String) {
    override fun toString(): String = inner
}

object Query {
    val foo = Foo(MyId("69420"), 42)
    val bar = Bar(MyId("42069"), URL("http://localhost:8080"))

    fun node() = if (Random.nextBoolean()) foo else bar

    fun foo() = foo

    fun bar() = bar

    suspend fun suspendFun() = 42

    fun futureFun(): CompletableFuture<Int> = CompletableFuture.completedFuture(42)

    fun deferedFun(): Deferred<Int> = CompletableDeferred(42)
}
