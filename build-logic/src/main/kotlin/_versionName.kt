import org.gradle.api.Project

/** Explicit CI version or an unpublished local development version; no Git dependency. */
internal val Project.versionName: String
    get() = rootProject.layout.buildDirectory.file("version-name.txt").get().asFile
        .takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.gradleProperty("releaseVersion").orNull
        ?: "0.0.0-SNAPSHOT"
