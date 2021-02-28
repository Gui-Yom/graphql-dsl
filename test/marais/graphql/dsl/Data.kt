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

abstract class Node(val id: String)

class MyData(id: String, val field: Int) : Node(id) {
    fun dec(): Int = field - 1
}

class OtherData(id: String, val field: URL) : Node(id) {

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

enum class MyEnum {
    VALUE0,
    VALUE1,
    VALUE2
}

object Query {
    val data = MyData("69420", 42)
    val otherdata = OtherData("42069", URL("http://localhost:8080"))

    fun node() = if (Random.nextBoolean()) data else otherdata

    fun data() = data

    fun otherdata() = otherdata

    suspend fun testSuspend() = 42

    fun testFuture(): CompletableFuture<Int> = CompletableFuture.completedFuture(42)

    fun testDeferred(): Deferred<Int> = CompletableDeferred(42)
}
