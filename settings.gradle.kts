pluginManagement {
    repositories {
        maven("https://kotlin.bintray.com/kotlin-eap")
        gradlePluginPortal()
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        mavenCentral()
        maven { setUrl("https://plugins.gradle.org/m2/") }
        jcenter()
    }
}
/*
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-multiplatform") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
        }
    }
}
*/
rootProject.name = "tmdb-lib"

enableFeaturePreview("GRADLE_METADATA")
