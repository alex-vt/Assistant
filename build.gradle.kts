import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version "1.8.0-1.0.8"
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
                implementation(compose.animation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)

                implementation("me.tatarka.inject:kotlin-inject-runtime:0.6.1")
                configurations["ksp"].dependencies.add(
                    project.dependencies.create(
                        "me.tatarka.inject:kotlin-inject-compiler-ksp:0.6.1"
                    )
                )

                api("moe.tlaster:precompose:1.3.15")

                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

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
            kotlin.srcDirs(
                "build/generated/ksp/android/androidDebug/kotlin",
                "build/generated/ksp/android/androidRelease/kotlin",
            )
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.0")
                implementation("androidx.activity:activity-compose:1.7.1")
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            kotlin.srcDirs(
                "build/generated/ksp/desktop/desktopMain/kotlin",
            )
            dependencies {
                implementation(compose.desktop.currentOs) {
                    // prevent kotlinx.coroutines.internal.FastServiceLoader.loadProviders()
                    // from loading Android version of Dispatchers.Main with higher priority than desktop
                    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-android")
                }
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

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        targetSdk = 33
    }

    val credentialsProperties = Properties()
    val credentialsPropertiesFile = file("credentials.properties")
    val isCredentialsExisting = credentialsPropertiesFile.exists()
    if (isCredentialsExisting) {
        credentialsProperties.load(FileInputStream(credentialsPropertiesFile))
    }

    signingConfigs {
        create("release") {
            storeFile =
                if (isCredentialsExisting) {
                    file(credentialsProperties["signingStoreLocation"] as String)
                } else {
                    null
                }
            storePassword = credentialsProperties["signingStorePassword"] as String
            keyAlias = credentialsProperties["signingKeyAlias"] as String
            keyPassword = credentialsProperties["signingKeyPassword"] as String
        }
    }

    buildTypes {
        val openAiApiKey = credentialsProperties["openAiApiKey"] as String
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("versionchanges.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        pickFirst("META-INF/*")
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res", "src/commonMain/resources")
        }
    }
}
