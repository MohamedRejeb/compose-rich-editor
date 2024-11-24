import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
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

    sourceSets.androidMain.dependencies {
        api(libs.androidx.appcompat)

        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.ktor.client.okhttp)
    }

    sourceSets.named("desktopMain").dependencies {
        implementation(compose.desktop.currentOs)

        implementation(libs.kotlinx.coroutines.swing)
        implementation(libs.ktor.client.okhttp)
    }

    sourceSets.iosMain.dependencies {
        implementation(libs.ktor.client.darwin)
    }

    sourceSets.jsMain.dependencies {
        implementation(libs.ktor.client.js)
    }

    sourceSets.wasmJsMain.dependencies {
        implementation(libs.ktor.client.wasm)
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
}