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
val ktxCoroutinesVersion: String by project
val reactiveStreamsVersion: String by project
val log4jVersion: String by project
val gqlVersion: String by project
val junitVersion: String by project

dependencies {
    // Kotlin
    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$ktxCoroutinesVersion"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")

    implementation("org.reactivestreams:reactive-streams:$reactiveStreamsVersion")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")

    // GraphQL
    api("com.graphql-java:graphql-java:$gqlVersion")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-core:$log4jVersion")
    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    testImplementation(project(":graphql-dsl-test"))
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
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.ExperimentalStdlibApi",
                "-Xopt-in=kotlin.reflect.jvm.ExperimentalReflectionOnLambdas",
                "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi"
            )
        }
    }

    test {
        useJUnitPlatform()
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
        create<MavenPublication>("graphql-dsl") {
            from(project.components["java"])
            pom {
                name.set(rootProject.name)
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
    sign(publishing.publications["graphql-dsl"])
}
