# graphql-dsl

Goal: build a graphql schema with a nice dsl, be the poweruser and control everything that gets generated, does nothing
under the hood by default, no (or minimal) runtime reflection (excluding schema building)

Stretch goal : Compile time schema building instead of runtime. Or at least a way to cook generated schema once at first
runtime then include it at the next compilation.

## Example

```kotlin

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

// Access the DSL
val schema = SchemaGenerator {

    // Define a scalar type given its Coercing implementation
    scalar("Url", UrlCoercing)

    // Declare MyId as a graphql ID scalar
    id<MyId>()

    // Define an enum directly based on its values
    enum<Baz>()

    // Define an input type
    input<Input>()

    // Define an interface backed by a kotlin interface / abstract class / sealed class
    !"This describes my Node interface"
    inter<Node> {
        derive()

        // Additional fields will be implemented by default on implementing types
        !"Description on a field too !"
        field("parent") { ->
            MyId("Node:" + id.inner)
        }
    }

    // Define a type backed by a kotlin class
    !"""
        This is a cool looking multiline description
        No need to call .trimIndent()
    """
    type<Foo> {
        // TODO specifying interface on a type should automatically declare appropriate fields
        inter<Node>()

        // Can be a property
        include(Foo::id)
        // Can be a member function
        include(Foo::dec)
        include(Foo::field)
        // Can be a custom function
        field("inc") { _: DataFetchingEnvironment ->
            field + 1
        }
    }

    type<Bar> {
        inter<Node>()

        // Automatically include properties and member functions
        derive()

        // Exclude a field
        -Bar::field

        field("custom") { param: String ->
            param
        }

        field("custom2") { a: List<Int>, b: Int ->
            a.map { it * b }
        }

        field("custom3") { a: Int, b: Float, c: MyId ->
            "$c: ${a * b}"
        }
    }

    // The main query object
    // Specify root query fields here
    query(Query) { derive() }
}.build()
```

## Features

- [x] Basic types + fields
- [x] Scalars
- [x] Interfaces (not as type checked as I would)
- [x] Field arguments
- [x] Input objects
- [x] Enums
- [x] CompletableFuture and Publisher support
- [x] Suspend and Flow support
- [x] Suspend in custom fields
- [ ] Non suspend custom fields
- [x] Schema element description
- [ ] Field argument default value (I don't think kotlin allows us to see that)
- [ ] Directive support
- [ ] Union types
- [ ] Multithreaded schema building (eg fire up a coroutine for each type to generate)
- [ ] Relay types builder (connection, edge, pageinfo), similar to graphql-java's relay helpers
- [ ] Ensure that as many checks as possible are done during the first step of schema building so errors are thrown with
  a useful line number (fail fast, maybe change the way the schema is generated)
