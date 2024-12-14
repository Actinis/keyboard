@file:Suppress("UnstableApiUsage")

import java.util.*

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties().apply {
    load(file(File(rootProject.projectDir, "local.properties")).reader())
}

val mavenUrl: String by localProperties
val mavenUser: String by localProperties
val mavenPassword: String by localProperties

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        maven {
            name = "privateMaven"
            url = uri(mavenUrl)
            credentials {
                username = mavenUser
                password = mavenPassword
            }
        }
    }
}

rootProject.name = "actinis-remote-keyboard"

include(":library")
include(":demo")
