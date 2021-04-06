# graphql-dsl

Goal: build a graphql schema with a nice dsl, be the poweruser and control everything that gets generated, does nothing
under the hood by default, no (or minimal) runtime reflection (excluding schema building)

Stretch goal : Compile time schema building instead of runtime. Or at least a way to cook generated schema once at first
runtime then include it at the next compilation.

## Example

```kotlin

abstract class Node(val id: String)

class Foo(id: String, val field: Int) : Node(id) {
    fun dec(): Int = field - 1
}

class Bar(id: String, val field: URL) : Node(id) {

    fun other(param: String): String = param
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

object Query {
    val data = MyData("69420", 42)
    val otherdata = OtherData("42069", URL("http://localhost:8080"))

    fun node() = if (Random.nextBoolean()) data else otherdata

    suspend fun suspending() = 42
}

// Access the DSL
val schema = SchemaGenerator {

    // Define a scalar type given its Coercing implementation
    scalar("Url", UrlCoercing)

    // Define an enum directly based on its values
    enum<Baz>()

    // Define an input type
    input<Input>()

    // Define an interface backed by a kotlin interface / abstract class / sealed class
    inter<Node> {
        derive()
    }

    // Define a type backed by a kotlin class
    type<Foo> {
        // Explicitly register that type to extend the given interface
        // Warning : you must manually include the fields required by the interface or use derive()
        // Warning : will probably change since not very ergonomic
        inter<Node>()

        // Register fields on this type
        // Can be a class property
        field(Foo::id)
        // Can be a member function
        field(Foo::dec)
        // Can be a custom field
        // Note the special argument here, it won't be included in the schema
        field("inc") { it: DataFetchingEnvironment ->
            field + 1
        }
    }

    type<Bar> {
        inter<Node>()

        // Automatically generate graphql fields based on public class properties and functions
        derive()

        // When using derive(), you can exclude fields you don't want
        exclude(Bar::other)

        // Custom fields can have any number of arguments
        field("custom") { a: Int, b: Float, c: String ->
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
- [x] Special types (CompletableFuture and Publisher)
- [x] Suspend and Flow support
- [ ] Complete type checking using reflection (especially interfaces)
- [x] Suspend in custom fields
- [ ] Non suspend custom fields
- [ ] Schema element description
- [ ] Directive support
- [ ] Union types
- [ ] Multithreaded schema building (eg fire up a coroutine for each type to generate)
- [ ] Relay types builder (connection, edge, pageinfo), similar to graphql-java's relay helpers
- [ ] Ensure that as many checks as possible are done during the first step of schema building so errors are thrown with
  a useful line number
