package com.servacode.directory.feature.navigation

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The voice, out loud.
 *
 * It says a cue with the recorded Arabic pack where there is a recording for it, and falls back
 * to the device's synthesiser where there is not. The fallback matters more than it looks: on
 * many phones no Arabic voice is installed at all, and a synthesiser with no voice fails
 * silently — guidance that says nothing and never explains why. The recordings are the reason
 * the common case never depends on that.
 *
 * Guidance speaks over other audio rather than pausing it, the way every navigation app does:
 * the instruction is short, and stopping someone's music for "انعطف يمينًا" is worse than
 * talking over it.
 */
@Singleton
class AndroidNavigationVoice @Inject constructor(
    @ApplicationContext private val context: Context,
) : NavigationVoice {
    private var ready = false
    private var pending: VoiceCue? = null
    private var player: MediaPlayer? = null
    private val tts = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { setLanguage() }
            pending?.let(::say)
            pending = null
        }
    }

    /** The words of a cue, for the synthesiser: the recordings say them in their own voice. */
    private val words = NavigationWords(context.resources)

    override fun say(cue: VoiceCue) {
        val clip = NavigationClips.clipFor(cue)
        if (clip != null && playClip(clip)) return
        speak(words.spoken(cue))
    }

    /**
     * True when the recording was found and started; false when there is none to play.
     *
     * The id comes from the generated table rather than from `Resources.getIdentifier`, which is
     * handed the application id — and every build but production carries a suffix on it, so the
     * lookup found nothing and guidance fell silently through to the synthesiser.
     */
    private fun playClip(name: String): Boolean {
        val id = NAVIGATION_CLIPS[name] ?: return false
        // One instruction at a time. A turn announced while the last sentence is still playing
        // replaces it: the newer one is the one that is still true.
        release()
        val created = runCatching { MediaPlayer.create(context, id) }.getOrNull() ?: return false
        // Marked as guidance so the system ducks music rather than being stopped by it. If the
        // device refuses the attributes the clip still plays, which is what matters.
        runCatching { created.setAudioAttributes(guidanceAttributes()) }
        created.setOnCompletionListener { release() }
        return runCatching { created.start(); player = created; true }
            .getOrElse { created.release(); false }
    }

    private fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "directory-navigation")
    }

    private fun setLanguage() {
        tts.language = Locale.forLanguageTag("ar")
        tts.setAudioAttributes(guidanceAttributes())
    }

    private fun guidanceAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    override fun stop() {
        pending = null
        release()
        runCatching { tts.stop() }
    }

    private fun release() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationVoiceModule {
    @Binds
    @Singleton
    abstract fun bindNavigationVoice(impl: AndroidNavigationVoice): NavigationVoice
}
