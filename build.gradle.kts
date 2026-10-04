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
}

java {
    // Java 25, not 21: the classes under commands/ use flexible constructor bodies,
    // which is a Java 25 preview feature. CI is pinned to 25 to match.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    // Flexible constructor bodies in commands/CommandFm subclasses are still preview,
    // so the compiler and the test JVM both need the flag.
    options.compilerArgs.addAll(listOf("--enable-preview", "-Xlint:all,-serial,-processing"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
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
