plugins {
    kotlin("multiplatform") version "1.3.21"
    id("kotlinx-serialization") version "1.3.21"
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    jcenter()
    mavenCentral()
    mavenLocal()
    maven( "https://dl.bintray.com/soywiz/soywiz")
    maven( "https://kotlin.bintray.com/kotlinx")
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("com.soywiz:klock:1.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:1.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.10.0")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        jvm("jvm").compilations["main"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.20")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")
            }
        }

        jvm("jvm").compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
    }
}

// for future functionality.
task("generateSecureProperties") {
    doLast {
        File("$projectDir/src/commonMain/resources", "secureProps.txt").apply {
            val apiKey = project.property("argus_tmdb_api_key") ?: "invalid!"
            writeText(apiKey.toString())
        }
    }
}


tasks.register<Copy>("copyResources") {
    from(file("$projectDir/src/jvmTest/resources/"))
    into(file("$buildDir/classes/kotlin/jvm/test/"))
}

tasks.get(name = "jvmTest").dependsOn += tasks.get(name = "copyResources")
