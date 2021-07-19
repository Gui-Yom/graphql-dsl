package marais.graphql.dsl

import marais.graphql.dsl.test.assertSchemaFails
import marais.graphql.dsl.test.withSchema
import kotlin.test.Test
import kotlin.test.assertTrue

class InputObjects {
    @Test
    fun `simple input object`() = withSchema({
        input<SimpleInput>()

        query {
            "test" { input: SimpleInput -> input.data }
        }
    }) {
        """
        query {
          test(input: { data: "hello" })
        }
        """ shouldReturns mapOf("test" to "hello")
    }

    @Test
    fun `nested input object`() = withSchema({
        input<SimpleInput>()
        input<NestedInput>()

        query {
            "test" { input: NestedInput -> "${input.data} ${input.nested.data}" }
        }
    }) {
        """
        query {
          test(input: { data: 69, nested: { data: "420" } })
        }
        """ shouldReturns mapOf("test" to "69 420")
    }

    @Test
    fun `nullable self reference input object`() = withSchema({
        input<SelfRefInput>()

        query {
            "test" { input: SelfRefInput ->
                sequence {
                    var acc: SelfRefInput? = input
                    while (acc != null) {
                        yield(acc.data)
                        acc = acc.nested
                    }
                }.joinToString(" ")
            }
        }
    }) {
        """
        query {
          test(input: { data: "hello", nested: { data: "world", nested: null } })
        }
        """ shouldReturns mapOf("test" to "hello world")
    }

    @Test
    fun `non-nullable self reference input object`() = assertSchemaFails {
        input<WrongSelfRefInput>()

        query {
            "test" { input: WrongSelfRefInput -> "bruh" }
        }
    }

    @Test
    fun `with fields not in constructor`() = withSchema({
        input<WithOtherFieldsInput>()

        query {
            "test" { input: WithOtherFieldsInput -> input.ignored }
        }
    }) {
        assertTrue("ignored" !in schema.print())
        assertTrue("ignored2" !in schema.print())
        """query { test(input: { data: "hello" }) }""" shouldReturns mapOf("test" to "hello !")
    }
}
