plugins {
    id("root.publication")
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
//    alias(libs.plugins.dokka).apply(false)
}

allprojects {
//    apply(plugin = "org.jetbrains.dokka")
}