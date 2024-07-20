import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    applyDefaultHierarchyTemplate()
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    jvm("desktop") {
        jvmToolchain(11)
    }
    js(IR).browser()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs().browser()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Common"
            isStatic = true
        }
    }

    sourceSets.commonMain.dependencies {
        api(compose.runtime)
        api(compose.foundation)
        api(compose.ui)
        api(compose.material)
        api(compose.material3)
        api(compose.materialIconsExtended)
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        implementation(compose.components.resources)

        implementation(projects.richeditorCompose)
        implementation(projects.richeditorComposeCoil3)

        // Voyager Navigator
        implementation(libs.voyager.navigator)

        // Coil
        implementation(libs.coil.compose)
        implementation(libs.coil.svg)
        implementation(libs.coil.network.ktor)

        // Ktor
        implementation(libs.ktor.client.core)
    }

    sourceSets {


        androidMain.dependencies {
            api("androidx.appcompat:appcompat:1.6.1")

            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        named("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)

            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        named("wasmJsMain").dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "com.mohamedrejeb.richeditor.sample.common"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        jvmToolchain(8)
    }
}