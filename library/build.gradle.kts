import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room)
}

group = "io.actinis.remote"
version = libs.versions.library.get()

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
//    iosX64()
//    iosArm64()
//    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit)

                implementation(libs.koin.core)
                implementation(libs.koin.core.coroutines)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.kotlinx.serialization.json)

                implementation(libs.settings)
                implementation(libs.settings.coroutines)

                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.androidx.room.runtime)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.components.resources)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(compose.preview)

                implementation(libs.koin.android)
                implementation(libs.androidx.startup.runtime)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }


        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.mockk)
            }
        }
    }
}

android {
    namespace = "io.actinis.remote.keyboard"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    debugImplementation(compose.uiTooling)

    add("kspAndroid", libs.androidx.room.compiler)
//    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
//    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), "keyboard", version.toString())

    pom {
        name = "Actinis Remote Keyboard"
        description = "Keyboard view for Actinis Remote"
        inceptionYear = "2024"
        url = "https://github.com/Actinis/keyboard"
        licenses {
            license {
                name = "Apache 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "Actinis"
                name = "Actinis OÜ"
                url = "https://github.com/Actinis"
            }
        }
        scm {
            url = "https://github.com/Actinis/keyboard"
            connection = "scm:git:git://github.com/Actinis/keyboard.git"
            developerConnection = "scm:git:ssh://git@github.com/Actinis/keyboard.git"
        }
    }
}
