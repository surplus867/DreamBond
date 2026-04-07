package com.example.dreambond.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale


class MinaVoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private val softVolume = 0.8f

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        val engine = tts ?: return
        val localeResult = engine.setLanguage(Locale.US)
        if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            return
        }

        // Prefer the highest quality US voice with lower latency when available.
        selectBestUsVoice(engine)?.let { bestVoice ->
            engine.voice = bestVoice
        }

        engine.setSpeechRate(0.96f)
        engine.setPitch(0.98f)
        isReady = true
    }

    fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, softVolume)
        }
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "mina_voice")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    private fun selectBestUsVoice(engine: TextToSpeech): Voice? {
        return engine.voices
            ?.asSequence()
            ?.filter { it.locale?.language == Locale.US.language }
            ?.maxWithOrNull(
                compareBy<Voice> { it.quality }
                    .thenByDescending { -it.latency }
            )
    }
}