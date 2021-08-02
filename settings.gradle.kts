pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
    }
}

rootProject.name = "graphql-dsl"

include(":graphql-dsl-test", ":graphql-codegen", ":examples:starwars", ":examples:codegen")
