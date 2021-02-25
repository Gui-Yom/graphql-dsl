# graphql-dsl

Goal: build a graphql schema with a nice dsl, be the poweruser and control everything that gets generated, does nothing
under the hood by default, no (or minimal) runtime reflection (excluding schema building)

Stretch goal : Compile time schema building instead of runtime. Or at least a way to cook generated schema once at first
runtime then include it at the next compilation.

## Roadmap

- [x] Basic types + fields
- [x] Scalars
- [x] Interfaces (not as type checked as I would)
- [x] Field arguments
- [ ] Input objects
- [ ] Special types (CompletableFuture and Publisher)
- [ ] Suspend and Flow support
- [ ] Union types
- [ ] Complete type checking using reflection
- [ ] Directive support