plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")
    alias(libs.plugins.jetbrains.compose.compiler)
    alias(libs.plugins.jetbrains.serialization)
}

kotlin {
    android {
        compileSdk = versionInt(libs.versions.build.sdk.compile)
        minSdk = versionInt(libs.versions.build.sdk.min)
        buildToolsVersion = versionString(libs.versions.build.tools)
    }
    sourceSets {
        commonTest.dependencies {
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.jetbrains.coroutines.test)
        }
        commonMain.dependencies {
            api(libs.jetbrains.compose.runtime)
            api(libs.jetbrains.coroutines.core)
            api(libs.jetbrains.serialization)
        }
    }
}

dokka.dokkaSourceSets.configureEach {
    sourceLink {
        localDirectory.set(projectDir.resolve("src"))
        remoteUrl("${env("POM_URL")}/tree/master/${project.name}/src")
    }
}
