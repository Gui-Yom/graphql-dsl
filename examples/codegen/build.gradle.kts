plugins {
    kotlin("jvm")
    kotlin("kapt")
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

val kotlinVersion: String by project
val ktxCoroutinesVersion: String by project
val log4jVersion: String by project

dependencies {
    // Kotlin
    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("stdlib-jdk8"))

    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$ktxCoroutinesVersion"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")

    // Logging
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    // GraphQL
    implementation(project(":"))
    kapt(project(":graphql-codegen"))
    compileOnly(project(":graphql-codegen"))
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

application {
    mainClass.set("MainKt")
}

kapt {
    includeCompileClasspath = false
    correctErrorTypes = true
    useBuildCache = false
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }
}
