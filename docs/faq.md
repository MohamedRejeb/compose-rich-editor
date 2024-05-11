# FAQ

Have a question that isn't part of the FAQ? Open an issue here [Compose-Rich-Editor](https://github.com/MohamedRejeb/Compose-Rich-Editor/issues).

## How do I get development snapshots?

Add the snapshots repository to your list of repositories in `build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
```

Or to your dependency resolution management in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}
```

Use the snapshot version:

```kotlin
implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-SNAPSHOT")
```

>Note: Snapshots are deployed for each new commit on `main` that passes CI. They can potentially contain breaking changes or may be unstable. Use at your own risk.