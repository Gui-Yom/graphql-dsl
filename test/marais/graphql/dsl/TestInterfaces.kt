package marais.graphql.dsl

import marais.graphql.dsl.test.withSchema
import kotlin.test.Test

class TestInterfaces {
    @Test
    fun `derive interfaces`() = withSchema({

        abstract class Parent(val a: Int)

        class Child(val data: String) : Parent(data.toInt())

        // Parent is declared before
        inter<Parent>()

        // We automatically infer that Child : Parent in graphql
        type<Child>()

        query {
            "test" { -> Child("42") }
        }
    }) {
        """query { test { data, a } }""" shouldReturns mapOf("test" to mapOf("data" to "42", "a" to 42))
    }
}
