package com.servacode.directory.feature.navigation

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
 *
 * **One sentence at a time, and never half of one.** A cue arriving while another is being
 * spoken used to replace it mid-word, which on a road with turns close together is most of what
 * a driver hears: the beginning of one instruction, then the beginning of the next. So a cue
 * that arrives during speech waits, and only one waits — a newer cue takes the waiting one's
 * place. That is what keeps a late instruction from being spoken after the turn it describes
 * has been taken: whatever is waiting when the voice frees up is, by construction, the most
 * recent thing guidance had to say.
 */
@Singleton
class AndroidNavigationVoice @Inject constructor(
    @ApplicationContext private val context: Context,
) : NavigationVoice {
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var pending: VoiceCue? = null
    private var player: MediaPlayer? = null

    /** Whether a sentence is being spoken right now, by either the recording or the synthesiser. */
    private var speaking = false

    /** The one cue that will be spoken next, replaced rather than queued behind by a newer one. */
    private var waiting: VoiceCue? = null

    /**
     * The synthesiser reports from a binder thread; everything here is touched on the main one,
     * so its answers are handed over rather than acted on where they arrive.
     *
     * Declared before the synthesiser that is given it, because that is the order the two are
     * built in.
     */
    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            main.post(::finished)
        }

        @Deprecated("The parameterless form is what older platforms call.")
        override fun onError(utteranceId: String?) {
            main.post(::finished)
        }
    }

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { configureSynthesiser() }
            pending?.let(::say)
            pending = null
        }
    }

    /** The words of a cue, for the synthesiser: the recordings say them in their own voice. */
    private val words = NavigationWords(context.resources)

    override fun say(cue: VoiceCue) {
        if (speaking) {
            waiting = cue
            return
        }
        start(cue)
    }

    private fun start(cue: VoiceCue) {
        speaking = true
        val clip = NavigationClips.clipFor(cue)
        if (clip != null && playClip(clip)) return
        if (!speak(words.spoken(cue))) finished()
    }

    /**
     * What to say once the current sentence has been said in full.
     *
     * Called when a recording ends, when the synthesiser reports it has finished or failed, and
     * when neither could say the cue at all — so the voice can never be left believing it is
     * still speaking, which would silence every instruction after it.
     */
    private fun finished() {
        release()
        speaking = false
        val next = waiting
        waiting = null
        next?.let(::start)
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
        release()
        val created = runCatching { MediaPlayer.create(context, id) }.getOrNull() ?: return false
        // Marked as guidance so the system ducks music rather than being stopped by it. If the
        // device refuses the attributes the clip still plays, which is what matters.
        runCatching { created.setAudioAttributes(guidanceAttributes()) }
        created.setOnCompletionListener { finished() }
        // A clip that cannot be played is not a silence to wait out: report it and let the
        // caller fall through to the synthesiser.
        created.setOnErrorListener { _, _, _ -> main.post(::finished); true }
        return runCatching { created.start(); player = created; true }
            .getOrElse { created.release(); false }
    }

    /** True when the synthesiser accepted the sentence and will report when it has said it. */
    private fun speak(text: String): Boolean {
        if (!ready) return false
        // Added rather than flushed: this class decides what follows what, and flushing here
        // would cut the sentence that the queue above just protected.
        val queued = runCatching {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
        }.getOrDefault(TextToSpeech.ERROR)
        return queued == TextToSpeech.SUCCESS
    }

    /** Language, audio attributes and the listener that says when a sentence has been said. */
    private fun configureSynthesiser() {
        tts.language = Locale.forLanguageTag("ar")
        tts.setAudioAttributes(guidanceAttributes())
        tts.setOnUtteranceProgressListener(progress)
    }

    private fun guidanceAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    override fun stop() {
        pending = null
        waiting = null
        speaking = false
        release()
        runCatching { tts.stop() }
    }

    private fun release() {
        player?.runCatching {
            setOnCompletionListener(null)
            setOnErrorListener(null)
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    private companion object {
        const val UTTERANCE_ID = "directory-navigation"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationVoiceModule {
    @Binds
    @Singleton
    abstract fun bindNavigationVoice(impl: AndroidNavigationVoice): NavigationVoice
}
