pluginManagement {
    repositories {
        google {
            mavenContent {
                releasesOnly()
            }
        }
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}


rootProject.name = "compose-richeditor"

include(
    ":sample:android",
    ":sample:desktop",
    ":sample:web",
    ":sample:common",
    ":richeditor-compose",
    ":markdown"
)
