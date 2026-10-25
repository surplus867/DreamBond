package com.example.dreambond.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class MinaVoiceManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private val softVolume = 0.8f

    // Observable by Compose so the portrait can animate while a character is talking.
    var isSpeaking by mutableStateOf(false)
        private set

    // Only the last queued utterance of a line ends the "speaking" state.
    @Volatile
    private var lastUtteranceId: String? = null

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            isSpeaking = true
        }

        override fun onDone(utteranceId: String?) {
            if (utteranceId == lastUtteranceId) isSpeaking = false
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (utteranceId == lastUtteranceId) isSpeaking = false
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            if (utteranceId == lastUtteranceId) isSpeaking = false
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        val engine = tts ?: return

        engine.setOnUtteranceProgressListener(progressListener)
        applyCharacterVoice(characterName = "Mina")
        isReady = true
    }

    // Updated speak() method to accept optional character name
    // If character is Clara, applies English/Canadian voice settings
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
        if (characterName == "Clara" && cleanText.contains("...")) {
            tts?.setSpeechRate(0.80f)
            // Drop pieces like a lone "?" so the engine never reads punctuation aloud.
            val parts = cleanText.split("...")
                .map { it.trim().trimStart('?', '!', '.', ',') }
                .filter { part -> part.any { it.isLetterOrDigit() } }
            lastUtteranceId = "character_voice_${parts.lastIndex}"
            if (parts.isEmpty()) {
                isSpeaking = false
                return
            }
            parts.forEachIndexed { i, part ->
                val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                tts?.speak(part, mode, params, "character_voice_$i")
                if (i < parts.lastIndex) {
                    tts?.playSilentUtterance(300, TextToSpeech.QUEUE_ADD, "pause_$i")
                }
            }
            return
        }
        lastUtteranceId = "character_voice"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "character_voice")
    }

    // Also keep the old speak(text: String) method for backward compatibility
    fun speak(text: String) {
        speak(null, text)
    }

    private fun applyCharacterVoice(characterName: String?) {
        val engine = tts ?: return

        when (characterName) {
            "Clara" -> {
                // Clara: English (Canadian)
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
        isSpeaking = false
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