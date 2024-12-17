package io.actinis.remote.keyboard.util

import java.util.*

enum class DesktopOs {
    LINUX,
    MACOS,
    WINDOWS,
}

fun getDesktopOs(): DesktopOs {
    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())

    return when {
        osName.contains("win") -> DesktopOs.WINDOWS
        osName.contains("mac") -> DesktopOs.MACOS
        else -> DesktopOs.LINUX
    }
}