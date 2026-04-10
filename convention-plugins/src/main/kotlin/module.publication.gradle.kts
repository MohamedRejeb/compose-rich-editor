plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), project.name, version.toString())

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
        developers {
            developer {
                id.set("MohamedRejeb")
                name.set("Mohamed Rejeb")
                email.set("mohamedrejeb445@gmail.com")
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
    }
}
