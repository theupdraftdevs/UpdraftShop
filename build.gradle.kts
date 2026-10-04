plugins {
    java
}

group = "updraftmc.shop"
version = "1.0.0"

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // The server provides Paper at runtime, so this must never end up in the jar.
    compileOnly(libs.paper.api)
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    // Paper 1.21.11 is compiled for Java 21, so that is the ceiling. Pinning it with
    // `release` rather than a toolchain means the build works on any JDK 21 or newer
    // instead of demanding one exact JDK be installed.
    options.release = 21
    options.compilerArgs.add("-Xlint:all,-serial,-processing")
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // Byte-identical output for the same source, so releases can be verified.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.processResources {
    val tokens = mapOf("version" to project.version.toString())

    inputs.properties(tokens)

    // plugin.yml is the single source of truth for the version at runtime.
    filesMatching("plugin.yml") {
        expand(tokens)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "UpdraftShop",
            "Implementation-Version" to project.version,
        )
    }
}

// Belongs in CI and locally, not in the published jar.
tasks.jar {
    exclude("**/*.DS_Store")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}