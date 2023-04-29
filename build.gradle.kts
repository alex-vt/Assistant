import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    android()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                implementation("androidx.compose.ui:ui:1.4.0-beta01")
                implementation("androidx.compose.ui:ui-tooling:1.4.0-beta01")
                implementation("androidx.compose.animation:animation:1.4.0-beta01")
                implementation("androidx.compose.animation:animation-core:1.4.0-beta01")
                implementation("androidx.compose.animation:animation-graphics:1.4.0-beta01")
                implementation("androidx.compose.foundation:foundation:1.4.0-beta01")
                implementation("androidx.compose.material:material:1.4.0-beta01")

                implementation("io.ktor:ktor-client-core:2.2.3")
                implementation("io.ktor:ktor-client-cio:2.2.3")
                implementation("io.ktor:ktor-client-content-negotiation:2.2.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.3")

                implementation("com.knuddels:jtokkit:0.4.0")

                implementation("net.sourceforge.tess4j:tess4j:5.6.0")
                implementation("com.github.tulskiy:jkeymaster:1.3")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))

                implementation("org.jetbrains.kotlin:kotlin-test:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

                implementation("io.mockk:mockk:1.12.1")
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            kotlin.srcDirs("src/jvmMain/kotlin")
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.9.0")
                implementation("androidx.activity:activity-compose:1.6.1")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.alexvt.assistant.Main_desktopKt"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        targetSdk = 33
    }

    val signingProperties = Properties()
    val signingPropertiesFile = file("signing.properties")
    val signingPropertiesExist = signingPropertiesFile.exists()
    if (signingPropertiesExist) signingProperties.load(FileInputStream(signingPropertiesFile))

    signingConfigs {
        create("release") {
            storeFile =
                if (signingPropertiesExist) file(signingProperties["signingStoreLocation"] as String) else null
            storePassword = signingProperties["signingStorePassword"] as String
            keyAlias = signingProperties["signingKeyAlias"] as String
            keyPassword = signingProperties["signingKeyPassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res", "src/commonMain/resources")
        }
    }
}
