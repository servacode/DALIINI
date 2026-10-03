package com.servacode.directory.feature.navigation

interface NavigationVoice {
    /**
     * Say a cue: with the recorded voice where the pack has one, and with the device's own
     * synthesiser where it does not.
     */
    fun say(cue: VoiceCue)

    fun stop()
}
