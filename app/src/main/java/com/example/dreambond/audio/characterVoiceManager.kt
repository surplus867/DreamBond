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

        applyCharacterVoice(characterName = "Mina")
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
                // Alice: English (Canadian)
                val localeResult = engine.setLanguage(Locale.CANADA)
                if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                    localeResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    engine.setLanguage(Locale.US)
                }
                selectBestEnglishVoice(engine, Locale.CANADA)
                    ?: selectBestEnglishVoice(engine, Locale.US)
                    ?: selectBestEnglishVoice(engine, Locale.ENGLISH)
                ?.let { bestVoice ->
                    engine.voice = bestVoice
                }
                engine.setSpeechRate(0.86f)
                engine.setPitch(1.08f)
            }
            else -> {
                // Mina: prefer natural US English to avoid accented fallback voices
                val localeResult = engine.setLanguage(Locale.US)
                if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                    localeResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    engine.setLanguage(Locale.ENGLISH)
                }
                selectBestEnglishVoice(engine, Locale.US)
                    ?: selectBestEnglishVoice(engine, Locale.ENGLISH)
                    ?: selectBestEnglishVoice(engine, Locale.CANADA)
                ?.let { bestVoice ->
                    engine.voice = bestVoice
                }
                engine.setSpeechRate(0.92f)
                engine.setPitch(1.0f)
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

    private fun selectBestEnglishVoice(engine: TextToSpeech, locale: Locale): Voice? {
        return engine.voices
            ?.asSequence()
            ?.filter { voice ->
                voice.locale?.language == locale.language
            }
            ?.filterNot { voice ->
                voice.isNetworkConnectionRequired
            }
            ?.sortedWith(
                compareBy<Voice> {
                    val name = it.name.lowercase()
                    val femaleHint = name.contains("female") || name.contains("feminine")
                    val neuralHint = name.contains("neural") || name.contains("wavenet")
                    when {
                        femaleHint && neuralHint -> 0
                        femaleHint -> 1
                        neuralHint -> 2
                        else -> 3
                    }
                }
                    .thenByDescending { it.quality }
                    .thenBy { it.latency }
            )
            ?.firstOrNull()
    }
}