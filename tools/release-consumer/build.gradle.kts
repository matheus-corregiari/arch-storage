plugins {
    kotlin("jvm") version "2.4.10"
    application
}

repositories {
    exclusiveContent {
        forRepository { mavenLocal() }
        filter { includeVersionByRegex("io.github.matheus-corregiari", "storage-.*", "1\\.0\\.0") }
    }
    google()
    mavenCentral()
}

val storageVersion = providers.gradleProperty("storageVersion").getOrElse("1.0.0")
dependencies {
    // RC16 did not expose storage-core on the backends' compile classpath.
    if (storageVersion == "2.0.0-rc16") {
        implementation("io.github.matheus-corregiari:storage-core:$storageVersion")
    }
    implementation("io.github.matheus-corregiari:storage-memory:$storageVersion")
    implementation("io.github.matheus-corregiari:storage-datastore:$storageVersion")
}
kotlin { jvmToolchain(21) }
application { mainClass.set("ConsumerKt") }
