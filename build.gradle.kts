plugins {
    //id "com.github.ben-manes.versions" version "0.20.0"
    kotlin("multiplatform") version "1.3.20-eap-52"
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    jcenter()
    mavenCentral()
    mavenLocal()
}

kotlin {
    /* Uncomment to work on native targets
    val windows = mingwX64("windows")
    val linux = linuxX64("linux")
    val mac = macosX64("mac") */

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("com.pajato.io:KFile:0.0.2")
                implementation("com.pajato.argus:ArgusCoreKML:0.0.1")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
            }
        }

        jvm("jvm").compilations["main"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.0")
            }
        }

        jvm("jvm").compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        /* Uncomment to wok on native targets
        sourceSets.create("nativeMain") {
            dependencies {
                implementation("com.pajato.io:KFile-native:0.0.2")
            }
        }

        configure(mutableListOf(windows, linux, mac)) {
            compilations["main"].defaultSourceSet.dependsOn(get("nativeMain"))
        } */
    }
}
/* Original Groovy script:
plugins {
    id("multiplatform") version "1.3.20-eap-52"
}

repositories {
    maven ("https://kotlin.bintray.com/kotlin-eap")
    mavenCentral()
}

group = "com.pajato.argus.cli"
version = "0.0.1"

val os = org.gradle.internal.os.OperatingSystem.current()


kotlin {
    val macos = macosX64("macos64")
    val linux = linuxX64("linux64")
    val windows = mingwX64("mingw64")

    val nativePreset = when {
        os.isWindows() -> presets.mingwX64
        os.isLinux() -> presets.linuxX64
        os.isMacOsX() -> presets.macosX64
        else -> /*unknown host*/ null
    }

    targets {
        fromPreset(presets.jvm, "jvm")
        // Not supported until someone writes the code to do so ... fromPreset(presets.js, "js")
        // For ARM, preset should be changed to presets.iosArm32 or presets.iosArm64
        // For Linux, preset should be changed to e.g. presets.linuxX64
        // For MacOS, preset should be changed to e.g. presets.macosX64
        //fromPreset(nativePreset, "native")
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                implementation("com.pajato.io:KFile:0.0.2")
                implementation("com.pajato.argus:ArgusCoreKML:0.0.1")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                testImplementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
                testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")
            }
        }
        jvmMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
            }
        }
        jvmTest {
            dependencies {
                testImplementation("org.jetbrains.kotlin:kotlin-test")
                testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
        nativeMain {
        }
        nativeTest {
        }
    }
}

task runProgram {
    def buildType = "release" // Change to "debug" to run application with debug symbols.
    dependsOn "link${buildType.capitalize()}ExecutableMacos"
    doLast {
        def programFile = kotlin.targets.macos.compilations.main.getBinary("EXECUTABLE", buildType)
        exec {
            executable programFile
            args = ""
        }
    }
}
*/
/*
dependencies {
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
*/
