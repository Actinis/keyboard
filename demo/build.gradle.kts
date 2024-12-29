import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ActinisKeyboardDemo"
            isStatic = true
        }
    }

    sourceSets {
        val jvmMain by creating {
            dependsOn(commonMain.get())

            dependencies {
            }
        }
        val desktopMain by getting {
            dependsOn(jvmMain)
        }
        val androidMain by getting {
            dependsOn(jvmMain)
        }

        commonMain.dependencies {
            implementation(libs.kermit)

            implementation(libs.koin.core)
            implementation(libs.koin.core.coroutines)
            implementation(libs.koin.compose)

            implementation(libs.kotlinx.serialization.json)

            implementation(libs.settings)
            implementation(libs.settings.coroutines)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(projects.library)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.koin.android)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
            }
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}

android {
    namespace = "io.actinis.remote.keyboard.demo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.actinis.remote.keyboard.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources.excludes.addAll(
            listOf(
                "META-INF/*",
            )
        )
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

tasks.withType<Test> {
    if (name == "mergeDebugAndroidTestAssets") {
        enabled = false
    }
}

tasks.withType<Test> {
    if (name == "copyRoomSchemasToAndroidTestAssetsDebugAndroidTest") {
        enabled = false
    }
}

tasks.whenTaskAdded {
    if (name.contains("copyRoomSchemasToAndroidTestAssetsDebugAndroidTest")) {
        enabled = false
    }
}

gradle.taskGraph.whenReady {
    allTasks.onEach { task ->
        if (task.name.contains("androidTest") || task.name.contains("connectedAndroidTest")) {
            task.enabled = false
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.actinis.remote.keyboard.demo.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.actinis.remote.keyboard.demo"
            packageVersion = "1.0.0"
        }
    }
}
