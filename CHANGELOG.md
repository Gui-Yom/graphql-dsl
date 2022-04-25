# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

[Unreleased]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.8.2...HEAD

### Added

- Set description on arguments right from the DSL without any annotations :
```kotlin
query {
  !"On field"
  !"arg: On argument"
  "test" { arg: Int -> 42 * a }
}
```
- Warn of unused description elements (overwritten description, unused arguments)
- Allow injecting GraphQLContext object directly

### Changed

- Updated Kotlin and dependencies :
  - kotlin 1.5.31 -> 1.6.21
  - kotlinx.coroutines 1.5 -> 1.6.1
  - log4j 2.14 -> 2.17
  - graphql-java 17.3 -> 18.0
  - junit 5.7 -> 5.8

### graphql-dsl-test

#### Added

- Allow customizing the ExecutionInput

## [0.8.2] - 2021-11-02

[0.8.2]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.8.1...v0.8.2

### Changed

- Updated dependencies (kotlin 1.5.31, graphql-java 17.3)
- Exclude non-public properties and functions from `derive`

## [0.8.1] - 2021-08-27

[0.8.1]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.8.0...v0.8.1

### Changed

- Updated dependencies (kotlin 1.5.30, graphql-java 17.2)

## [0.8.0] - 2021-08-06

[0.8.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.7.1...v0.8.0

### Added

- `doNotConvertFlowToPublisher()` schema building directive that does what it says.

### Changed

- Artifact group is now `marais.graphql`

### Fixed

- Correctly handle the case were a fetcher returns Flow\<Map\>
- Support CompletableFuture again

## [0.7.1] - 2021-08-01

[0.7.1]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.7.0...v0.7.1

### Fixed

- Support Flow for subscriptions back again
- Map kotlin.Long to GraphQLInt by default

## [0.7.0] - 2021-08-01

[0.7.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.6.0...v0.7.0

### Added

- Support descriptions on derived fields, and any other primitive, with @GraphQLDescription annotations. The dsl
  description always has priority.
- `GraphQLSchema()` builder function, equivalent to `SchemaBuilder().build()`
- Migrate from logback to log4j2
- Automatically derive interfaces on object types if the interface is declared before

### Fixed

- The function receiver wasn't passed to the fetcher under some conditions

## [0.6.0] - 2021-07-19

[0.6.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.5.0...v0.6.0

### Added

- Support list of input objects
- Support enum values as input

### Fixed

- Correctly derive input object fields from primary constructor
- Fix self referencing input objects

## [0.5.0] - 2021-07-18

[0.5.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.4.0...v0.5.0

### Added

- Id type coercer can be automatically derived from the class constructor assuming such constructor
  exists : `constructor(raw: String)`
- New notations for fields and for root objects :

```kotlin
query { }
// is equivalent to
query(object {}) { }

query {
    "fieldname" { -> "Yay" }
    // is equivalent to
    field("fieldname") { -> "Yay" }
}
```

- New test framework for your graphql code based on graphql-dsl under `/graphql-dsl-test`
- Working input objects, can reference other input objects, can self reference, no list of input object
- Allow default name for scalars

### Fixed

- Partial fix for fields that return Map
- Custom fields in root objects

## [0.4.0] - 2021-07-14

[0.4.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.3.2...v0.4.0

### Added

- Experimental automatic MapEntry type generation
- print() extension to GraphQLSchema

### Changed

- Field exclusion now happens before actually including the field, directly at derive step

## [0.3.2] - 2021-07-11

[0.3.2]: https://github.com/Gui-Yom/graphql-dsl/releases/tag/v0.3.2
