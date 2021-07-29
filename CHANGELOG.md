# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Support descriptions on derived fields, and any other primitive, with @GraphQLDescription annotations. The dsl
  description always has priority.
- `GraphQLSchema()` builder function, equivalent to `SchemaBuilder().build()`
- Migrate from logback to log4j2
- Automatically derive interfaces on object types if the interface is declared before

## [0.6.0]

### Added

- Support list of input objects
- Support enum values as input

### Fixed

- Correctly derive input object fields from primary constructor
- Fix self referencing input objects

## [0.5.0]

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

### Added

- Experimental automatic MapEntry type generation
- print() extension to GraphQLSchema

### Changed

- Field exclusion now happens before actually including the field, directly at derive step

## [0.3.2] - 2021-07-11

[Unreleased]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.6.0...HEAD

[0.6.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.5.0...v0.6.0

[0.5.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.4.0...v0.5.0

[0.4.0]: https://github.com/Gui-Yom/graphql-dsl/compare/v0.3.2...v0.4.0

[0.3.2]: https://github.com/Gui-Yom/graphql-dsl/releases/tag/v0.3.2
