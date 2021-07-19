package marais.graphql.dsl

import marais.graphql.dsl.test.withSchema
import kotlin.test.Test

class TestInputs {
    @Test
    fun `simple input`() = withSchema({
        query {
            "test" { a: Int -> 2 * a }
        }
    }) {
        """query { test(a: 21) }""" shouldReturns mapOf("test" to 42)
    }

    @Test
    fun `nullable input`() = withSchema({
        query {
            "test" { key: String? -> key ?: "null" }
        }
    }) {
        """query { first: test(key: "notnull"), second: test(key: null) }""" shouldReturns mapOf(
            "first" to "notnull",
            "second" to "null"
        )
    }

    @Test
    fun `enum input`() = withSchema({
        enum<Baz>()

        query {
            "test" { value: Baz -> value.ordinal }
        }
    }) {
        """query { first: test(value: VALUE0) }""" shouldReturns mapOf("first" to 0)
    }

    @Test
    fun `list input`() = withSchema({
        query {
            "test" { numbers: List<Int> -> numbers.map { it.toString() } }
        }
    }) {
        """query { test(numbers: [0, 1, 2, 3]) }""" shouldReturns mapOf("test" to listOf("0", "1", "2", "3"))
    }

    @Test
    fun `list of input objects`() {

        data class MyInput(
            val data: String,
        )

        withSchema({
            input<MyInput>()

            query {
                "test" { inputs: List<MyInput> -> inputs.map { it.data }.joinToString(" ") }
            }
        }) {
            """query { test(inputs: [{ data: "hello" }, { data: "world" }]) }""" shouldReturns mapOf(
                "test" to "hello world"
            )
        }
    }

    @Test
    fun `primitive array input`() = withSchema({
        query {
            // Array<Int> gets compiled to IntArray
            "test" { numbers: IntArray -> numbers.sortedArray() }
            "test2" { numbers: Array<Int> -> numbers.sortedArray() }
        }
    }) {
        """query { test(numbers: [2, 1, 0, 3]), test2(numbers: [2, 1, 0, 3]) }""" shouldReturns mapOf(
            "test" to arrayOf("0", "1", "2", "3"),
            "test2" to arrayOf("0", "1", "2", "3")
        )
    }

    @Test
    fun `object array input`() = withSchema({
        query {
            "test" { values: Array<String> -> values.joinToString(separator = " ") }
        }
    }) {
        """query { test(values: ["hello", "world"]) }""" shouldReturns mapOf("test" to "hello world")
    }
}
