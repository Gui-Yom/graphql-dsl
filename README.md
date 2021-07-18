# graphql-dsl

Goal: build a graphql schema with a nice dsl, be the poweruser and control everything that gets generated, does nothing
under the hood by default, no (or minimal) runtime reflection (excluding schema building)

Stretch goal : Compile time schema building instead of runtime. Or at least a way to cook generated schema once at first
runtime then include it at the next compilation.

## Example

```kotlin
val schema = SchemaBuilder {

    // Define a scalar type given its Coercing implementation
    scalar("Url", UrlCoercing)

    // Declare MyId as a graphql ID scalar
    id<MyId>()

    // Define an enum directly based on its values
    enum<Baz>()

    // Define an input type
    input<Input>()

    // Define an interface backed by a kotlin interface / abstract class / sealed class / open class
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
        No need to .trimIndent()
    """
    type<Foo> {
        inter<Node>()

        // Can be a property
        include(Foo::id)
        // Can be a member function
        // We can rename fields too
        include(Foo::dec, "decrement")
        +Foo::field
        // Can be a custom function
        field("inc") { _: DataFetchingEnvironment ->
            field + 1
        }
    }

    type<Bar> {
        inter<Node>()

        // Automatically include properties and member functions
        derive {
            // Exclude a field
            -Bar::field
        }

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
    // Here we derive our root query object from a kotlin object
    // object Query {
    //   fun allFoos(): List<Foo> = ...
    // }
    query(Query) { derive() }

    // Also
    type<Bar>()
    // is equivalent to
    type<Bar> { derive() }
    // Same with every other type builders
}.build() // Returns a ready to use GraphQLSchema
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
- Cache lookups to input objects constructors and parameters
