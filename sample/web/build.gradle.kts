plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
}

compose {
    kotlinCompilerPlugin.set(libs.versions.compose.compiler)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":sample:common"))
            }
        }
    }
}

compose.experimental {
    web.application {}
}