package com.servacode.directory.feature.navigation

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNavigationVoice @Inject constructor(
    @ApplicationContext context: Context,
) : NavigationVoice {
    private var ready = false
    private var pending: String? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts.language = Locale("ar")
                pending?.let(::speak)
                pending = null
            }
        }
    }

    override fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "directory-navigation")
    }

    override fun stop() {
        pending = null
        tts.stop()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationVoiceModule {
    @Binds
    @Singleton
    abstract fun bindNavigationVoice(impl: AndroidNavigationVoice): NavigationVoice
}
