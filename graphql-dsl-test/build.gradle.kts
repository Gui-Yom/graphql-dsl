plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

repositories {
    mavenLocal()
    mavenCentral()
}

val kotlinVersion: String by project
val gqlVersion: String by project

dependencies {
    // Kotlin
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(kotlin("test-junit5", kotlinVersion))

    // GraphQL
    implementation("com.graphql-java:graphql-java:$gqlVersion")
    // GraphQL DSL
    implementation(project(":"))
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
    test {
        java.srcDir("test")
        resources.srcDir("testresources")
    }
}

kotlin {
    sourceSets["main"].kotlin.srcDirs("src")
    sourceSets["test"].kotlin.srcDirs("test")
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }
}

fun RepositoryHandler.githubPackages(path: String) = maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/$path")
    credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
        password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
    }
}

publishing {
    repositories {
        githubPackages("Gui-Yom/graphql-dsl")
    }
    publications {
        create<MavenPublication>("graphql-dsl-test") {
            from(project.components["java"])
            pom {
                name.set("graphql-dsl-test")
                description.set("Test your GraphQL code with a code-first Kotlin DSL")
                url.set("https://github.com/Gui-Yom/graphql-dsl")
                developers {
                    developer {
                        id.set("Gui-Yom")
                        name.set("Guillaume Anthouard")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Gui-Yom/graphql-dsl.git")
                    developerConnection.set("scm:git:ssh://github.com/Gui-Yom/graphql-dsl.git")
                    url.set("https://github.com/Gui-Yom/graphql-dsl/")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["graphql-dsl-test"])
}
