package io.actinis.remote.keyboard.util

import co.touchlab.kermit.Logger
import java.io.File
import java.nio.file.Paths

private const val APP_NAME = "actinis-remote-keyboard"

private val logger = Logger.withTag("PathsUtil")

internal fun getUserHomeDirectory(): String {
    return System.getProperty("user.home")
}

internal fun getDataDirectory(): File {
    val userHome = getUserHomeDirectory()

    val path = when (getDesktopOs()) {
        DesktopOs.LINUX -> Paths.get(userHome, ".local", "share", APP_NAME)
        DesktopOs.MACOS -> Paths.get(userHome, "Library", "Application Support", APP_NAME)
        DesktopOs.WINDOWS -> Paths.get(userHome, "AppData", "Local", APP_NAME)
    }

    val file = path.toFile()
    if (!file.exists()) {
        logger.d { "Data directory == \"${file.absolutePath}\" does not exist, will create" }

        if (!file.mkdirs()) {
            logger.e { "Failed to create directory \"${file.absolutePath}\"" }
        }
    }

    return file
}