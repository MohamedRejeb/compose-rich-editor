rootProject.name = "compose-richeditor"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("convention-plugins")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val isJitPack = System.getenv("JITPACK") != null

include(
    ":richeditor-compose",
    ":richeditor-compose-coil3",
)

if (!isJitPack) {
    include(
        ":sample:android",
        ":sample:desktop",
        ":sample:web",
        ":sample:common",
    )
}
