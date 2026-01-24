import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `maven-publish`
    signing
}

publishing {
    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(tasks.register("${name}JavadocJar", Jar::class) {
            archiveClassifier.set("javadoc")
            archiveAppendix.set(this@withType.name)
        })

        // Provide artifacts information required by Maven Central
        pom {
            name.set("Compose Rich Editor")
            description.set("A Compose multiplatform library that provides a rich text editor.")
            url.set("https://github.com/MohamedRejeb/Compose-Rich-Editor")

            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://opensource.org/licenses/Apache-2.0")
                }
            }
            issueManagement {
                system.set("Github")
                url.set("https://github.com/MohamedRejeb/Compose-Rich-Editor/issues")
            }
            scm {
                connection.set("https://github.com/MohamedRejeb/Compose-Rich-Editor.git")
                url.set("https://github.com/MohamedRejeb/Compose-Rich-Editor")
            }
            developers {
                developer {
                    id.set("MohamedRejeb")
                    name.set("Mohamed Rejeb")
                    email.set("mohamedrejeb445@gmail.com")
                }
            }
        }
    }
}

val signingKeyId = System.getenv("OSSRH_GPG_SECRET_KEY_ID")
val signingKey = System.getenv("OSSRH_GPG_SECRET_KEY")
val signingPassword = System.getenv("OSSRH_GPG_SECRET_KEY_PASSWORD")
val hasSigning = !signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()

if (hasSigning) {
    signing {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
} else {
    // JitPack / CI builds usually do not have signing keys. Disable signing tasks.
    tasks.withType(Sign::class.java).configureEach {
        enabled = false
    }
}

// TODO: remove after https://youtrack.jetbrains.com/issue/KT-46466 is fixed
project.tasks.withType(AbstractPublishToMaven::class.java).configureEach {
    if (hasSigning) {
        dependsOn(project.tasks.withType(Sign::class.java))
    }
}
