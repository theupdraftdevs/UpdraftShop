plugins {
    // Deliberately not the `application` plugin: this is a Bukkit plugin, so there is
    // no main class to run and no start scripts or dist archives to ship.
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

    // Tests run off-server, so they need an implementation on the classpath.
    testImplementation(libs.paper.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    // NOT a toolchain. Paper 1.21.11 runs on Java 21 and rejects class files newer
    // than major 65, so `release` pins the bytecode to exactly what the server can
    // load while letting any JDK 21 or newer run the build.
    options.release = 21
    options.compilerArgs.add("-Xlint:all,-serial,-processing")
}

tasks.test {
    useJUnitPlatform()

    // The bytecode compatibility check reads the jar, so it has to run after it is
    // built rather than alongside compilation.
    dependsOn(tasks.jar)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    // Byte-identical output for the same source, so a release can be verified.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.processResources {
    val tokens = mapOf("version" to project.version.toString())

    inputs.properties(tokens)

    // plugin.yml is the single source of truth for the version at runtime, so it can
    // never drift from the tag or the jar name.
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
