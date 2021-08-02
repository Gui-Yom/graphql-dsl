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
val kotlinPoetVersion: String = "1.9.+"

dependencies {
    // Kotlin
    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.squareup:kotlinpoet:$kotlinPoetVersion")
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
        create<MavenPublication>("graphql-codegen") {
            from(project.components["java"])
            pom {
                name.set("graphql-codegen")
                description.set("Generate your GraphQL schema with a code-first Kotlin DSL")
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
    sign(publishing.publications["graphql-codegen"])
}
