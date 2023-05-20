pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
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
    ":richeditor-compose"
)
