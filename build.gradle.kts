plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

val kotlinVersion: String by project
val ktxCoroutinesVersion: String by project
val slf4jVersion: String by project
val gqlVersion: String by project
val junitVersion: String by project

dependencies {
    // Kotlin
    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$ktxCoroutinesVersion"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    // GraphQL
    implementation("com.graphql-java:graphql-java:$gqlVersion")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
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
            useIR = true
            jvmTarget = JavaVersion.VERSION_11.toString()
            //javaParameters = true
            //freeCompilerArgs = listOf("-Xemit-jvm-type-annotations")
        }
    }

    test {
        useJUnitPlatform()
    }

    publishing {
        publications {
            create<MavenPublication>("mercuri-core") {
                from(project.components["java"])
                pom {
                    name.set(rootProject.name)
                    description.set("")
                    url.set("https://github.com/Mercuri-Inc/mercuri-core")
                    developers {
                        developer {
                            id.set("Gui-Yom")
                            name.set("Guillaume Anthouard")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/Mercuri-Inc/mercuri-core.git")
                        developerConnection.set("scm:git:ssh://github.com/Mercuri-Inc/mercuri-core.git")
                        url.set("https://github.com/Mercuri-Inc/mercuri-core/")
                    }
                }
            }
        }
    }
}
