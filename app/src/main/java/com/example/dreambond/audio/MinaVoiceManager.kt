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

        val locale = Locale.KOREAN
        val localeResult = engine.setLanguage(locale)
        if (localeResult ==
            TextToSpeech.LANG_MISSING_DATA ||
            localeResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return
        }

        selectBestKoreanVoice(engine)?.let { bestVoice ->
            engine.voice = bestVoice
        }

        engine.setSpeechRate(0.84f)
        engine.setPitch(1.18f)
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

    private fun selectBestKoreanVoice(engine: TextToSpeech): Voice? {
        return engine.voices
            ?.asSequence()
            ?.filter { voice ->
                voice.locale?.language == Locale.KOREAN.language
            }
            ?.sortedWith(
                compareBy<Voice> {
                    // Prefer voices that might be female (name hint)
                    val name = it.name.lowercase()
                    !(name.contains("female") || name.contains("feminine"))
                }
                    .thenByDescending { it.quality }
                    .thenBy { it.latency }
            )
            ?.firstOrNull()
    }
}