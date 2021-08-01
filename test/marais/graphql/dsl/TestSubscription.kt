package marais.graphql.dsl

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import marais.graphql.dsl.test.withSchema
import org.reactivestreams.Publisher
import kotlin.test.Test
import kotlin.test.assertIs

class TestSubscription {

    @Test
    fun `flow as publisher`() = withSchema({
        // Schema must have a query type
        query {
            "dummy" { -> 0 }
        }

        subscription {
            "counter" { from: Int, to: Int, delayMs: Long ->
                flow {
                    for (i in from..to) {
                        delay(delayMs)
                        emit(i)
                    }
                }
            }
        }
    }) {
        withQuery("""subscription { counter(from: 0, to: 10, delayMs: 100) }""") {
            assertIs<Publisher<Int>>(getData<Any?>())
        }
    }
}
