pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
    }
}

rootProject.name = "graphql-dsl"

include(":graphql-dsl-test", ":examples:starwars")
