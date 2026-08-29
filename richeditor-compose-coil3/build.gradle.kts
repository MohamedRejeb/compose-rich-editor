@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.bcv)
    id("module.publication")
}

kotlin {
    explicitApi()
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.mohamedrejeb.richeditor.compose.coil"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    js {
        browser {
            testTask {
                enabled = false
            }
        }
        binaries.executable()
    }

    wasmJs {
        browser {
            testTask {
                enabled = true
            }
        }
        binaries.executable()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets.commonMain.dependencies {
        implementation(projects.richeditorCompose)

        implementation(libs.compose.ui)
        implementation(libs.compose.foundation)

        implementation(libs.coil.compose)
    }

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}