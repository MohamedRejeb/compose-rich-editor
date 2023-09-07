plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

allprojects {
    group = "com.mohamedrejeb.richeditor"
    version = "1.0.0-beta03"
}

nexusPublishing {
    // Configure maven central repository
    // https://github.com/gradle-nexus/publish-plugin#publishing-to-maven-central-via-sonatype-ossrh
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            stagingProfileId.set(System.getenv("OSSRH_STAGING_PROFILE_ID"))
            println("OSSRH_USERNAME")
            println(System.getenv("OSSRH_USERNAME"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}
