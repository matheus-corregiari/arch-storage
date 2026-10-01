plugins {
    id("arch-multi-library")
    id("arch-lint")
    id("arch-documentation")
    id("arch-optimize")
    id("arch-publish")
}

kotlin {
    android {
        compileSdk {
            version = release(versionInt(libs.versions.build.sdk.compile)) {
                minorApiLevel = versionInt(libs.versions.build.sdk.minor)
            }
        }
        minSdk = versionInt(libs.versions.build.sdk.min)
        buildToolsVersion = versionString(libs.versions.build.tools)
    }
    sourceSets {
        commonTest.dependencies {
            implementation(libs.jetbrains.kotlin.test)
            implementation(libs.jetbrains.coroutines.test)
        }
        commonMain.dependencies {
            api(project(":storage-core"))
        }
        val opMain = create("opMain") {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.androidx.datastore.core)
                api(libs.androidx.datastore.preferences)
                implementation(libs.arch.lumber)
            }
        }
        val noopMain = create("noopMain") { dependsOn(commonMain.get()) }
        androidMain { dependsOn(opMain) }
        jvmMain { dependsOn(opMain) }
        appleMain { dependsOn(opMain) }
        webMain { dependsOn(noopMain) }
        val opTest = create("opTest") { dependsOn(commonTest.get()) }
        jvmTest { dependsOn(opTest) }
        appleTest { dependsOn(opTest) }
        named("androidHostTest") { dependsOn(opTest) }
    }
}

dokka.dokkaSourceSets.configureEach {
    sourceLink {
        localDirectory.set(projectDir.resolve("src"))
        remoteUrl("${env("POM_URL")}/tree/master/${project.name}/src")
    }
}
