package marais.graphql.dsl

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import java.net.URL

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
