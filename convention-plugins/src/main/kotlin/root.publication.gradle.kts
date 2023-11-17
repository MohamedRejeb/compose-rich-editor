plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

allprojects {
    group = "com.mohamedrejeb.richeditor"
    version = "1.0.0-beta04"
}

nexusPublishing {
    // Configure maven central repository
    // https://github.com/gradle-nexus/publish-plugin#publishing-to-maven-central-via-sonatype-ossrh
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
//            stagingProfileId.set(System.getenv("OSSRH_STAGING_PROFILE_ID"))
//            username.set(project.findProperty("sonatypeUsername")?.toString()?.replaceFirst("'", "")?.replaceFirst("'", ""))
//            password.set(project.findProperty("sonatypePassword")?.toString()?.replaceFirst("'", "")?.replaceFirst("'", ""))
            stagingProfileId.set(System.getenv("OSSRH_STAGING_PROFILE_ID"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}
