# graphql-dsl

Build your GraphQL schema with a declarative Kotlin DSL.

See also [graphql-dsl-test](#graphql-dsl-test) for some test utilities.

## Example

```kotlin
val schema = SchemaBuilder {

    // Declare a scalar type given its Coercing implementation
    scalar(UrlCoercing, "Url")

    // Declare MyId as a graphql ID scalar
    id<MyId>()

    // Declare an enum directly based on its values
    enum<Baz>()

    // Declare an input type
    input<Input>()

    // Declare an interface
    !"This describes my Node interface"
    inter<Node> {
        // Construct the interface fields from the class member functions 
        derive()
    }

    // Define a type backed by a kotlin class
    !"""
        This is a cool looking multiline description
        No need to .trimIndent()
    """
    type<Foo> {
        inter<Node>()

        // Can be a property
        include(Foo::id)
        // Can be a member function
        // We can rename fields too
        include(Foo::dec, "decrement")
        // Shorthand notation
        +Foo::field
        // Can be a custom field
        // The lambda uses Foo as receiver
        field("inc") { -> field + 1 }
    }

    type<Bar> {
        inter<Node>()

        // Automatically include properties and member functions
        derive {
            // Exclude a field
            -Bar::field
        }

        // Use the input type we defined earlier
        field("custom") { param: Input ->
            param
        }

        // List are expected to work too !
        field("custom2") { a: List<Int>, b: Int ->
            a.map { it * b }
        }

        // No need for field()
        // You can declare fields using the invoke operator defined on String in this context
        "custom3" { a: Int, b: Float, c: MyId ->
            "$c: ${a * b}"
        }

        // Custom fields support up to 6 arguments

        // You can inject special arguments
        // They won't be included in the schema
        "consumesEnv" { env: DataFetchingEnvironment -> env.executionId }
    }

    // The main query object
    // Specify root query fields here
    // Here we derive our root query object from a kotlin object
    // object Query {
    //   fun allFoos(): List<Foo> = ...
    // }
    query(Query)

    // No need for a Kotlin object, define your query type directly with custom fields
    query {
        "answer" { -> 42 }
    }

    // Also
    type<Bar>()
    // is directly equivalent to
    type<Bar> { derive() }
    // Same with every other type builders
}.build() // Returns a ready to use GraphQLSchema

// You can use the GraphQLSchema.print() extension to render your schema to a String
println(schema.print())
```

## Features

- All operations (query, mutation, subscription)
- Object types, backed by a Kotlin type
- Custom scalars
- Interfaces, backed by a Kotlin supertype
- Input objects, backed by a kotlin data class, can reference other input objects including self
- Enums, backed by a Kotlin enum class
- Derive fields for types automatically from member properties, functions and custom exclusion rules
- Suspend, Deferred and CompletableFuture support for async fields
- Flow and Publisher support for subscription fields
- Suspend custom fields (fields without a property or a function)
- Schema descriptions directly in the DSL (no descriptions on derived fields)
- Map type support through automatic conversion to List<MapEntry>

## Planned features

- Support injecting GraphQLContext field parameter
- Non suspend custom fields
- Description on derived fields (need annotations)
- Support primitive arrays and object arrays
- Support list of input objects
- Field argument default value (need annotations)
- Support generics types (throw exception on *-projection, type arguments must be declared in the schema,
  monomorphisation)
- Support custom map entries (name, key name, value name)
- Directive support
- Union types
- Relay types builders (connection, edge, pageinfo), similar to graphql-java's relay helpers
- Ensure that as many checks as possible are done during the first step of schema building so errors are thrown with a
  useful line number (fail fast, maybe change the way the schema is generated)
- Map everything at initialization so minimal work is done at runtime
- Explore a way to verify and generate the schema at compile time through a compiler plugin or a gradle plugin

## Installation

Artifacts are only published to Github Packages. With Gradle :

```kotlin
repositories {
    maven {
        setUrl("https://maven.pkg.github.com/Gui-Yom/graphql-dsl")
        credentials { ... }
    }
}
dependencies {
    testImplementation("marais:graphql-dsl:0.5.0")
}
```

# graphql-dsl-test

The `graphql-dsl-test` artifact includes small utilities to make testing your schema code through graphql queries
easier.

## Example

```kotlin
@Test
fun testMyQuery() = withSchema({
        query {
            "test" { a: Int -> 2 * a }
        }
    }) {
        // The above schema will be printed through a debug logger
        // The receiver exposes the following properties and functions

        // Access the GraphQL instance and the schema
        graphql.execute("query { }")
        assertTrue(schema.typeMap.contains("Query"))

        // Execute a query and do something with the ExecutionResult as receiver
        withQuery("""query { test(a: 21) }""") {
            assertTrue(errors.isEmpty())
        }

        // This checks that there are no errors and print them in case of failure
        assertQueryReturns("""query { test(a: 21) }""", mapOf("test" to 42))
        // also equivalent to
        """query { test(a: 21) }""" shouldReturns mapOf("test" to 42)
    }
```

## Installation

Artifacts are only published to Github Packages. With Gradle :

```kotlin
repositories {
    maven {
        setUrl("https://maven.pkg.github.com/Gui-Yom/graphql-dsl")
        credentials { ... }
    }
}
dependencies {
    testImplementation("marais:graphql-dsl-test:0.5.0")
}
```
