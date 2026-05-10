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

        // Initialize with default Korean locale for Mina
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

    // Updated speak() method to accept optional character name
    // If character is "Alice", applies English/Canadian voice settings
    // Otherwise uses default Mina Korean voice settings
    fun speak(characterName: String?, text: String) {
        if (!isReady || text.isBlank()) return

        // Apply character-specific voice settings before speaking
        applyCharacterVoice(characterName)

        // Strip emoji so TTS doesn't read them aloud
        val cleanText = text.replace(Regex("[\\p{So}\\p{Cn}\\p{Cs}\\p{Co}]+"), "").trim()
        if (cleanText.isBlank()) return

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, softVolume)
        }
        tts?.stop()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "character_voice")
    }

    // Also keep the old speak(text: String) method for backward compatibility
    fun speak(text: String) {
        speak(null, text)
    }

    private fun applyCharacterVoice(characterName: String?) {
        val engine = tts ?: return

        when (characterName) {
            "Alice" -> {
                // Alice: English (Canadian) voice
                val localeResult = engine.setLanguage(Locale.CANADA)
                if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                    localeResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Fallback to English if Canadian not available
                    engine.setLanguage(Locale.ENGLISH)
                }
                engine.setSpeechRate(0.86f)
                engine.setPitch(1.08f)
            }
            else -> {
                // Mina: Korean voice (default)
                engine.setLanguage(Locale.KOREAN)
                selectBestKoreanVoice(engine)?.let { bestVoice ->
                    engine.voice = bestVoice
                }
                engine.setSpeechRate(0.84f)
                engine.setPitch(1.18f)
            }
        }
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