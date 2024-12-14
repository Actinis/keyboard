package io.actinis.remote.keyboard.domain.debug

interface IsDebug {
    fun execute(): Boolean
}

internal class IsDebugImpl : IsDebug {

    override fun execute(): Boolean {
        return true
    }
}