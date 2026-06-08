package com.example.dreambond

import com.example.dreambond.model.DialogueOption
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Manages dialogue generation and character-specific responses.
 * Handles dynamic replies, personality detection, memory recall lines, and goodnight messages.
 * This keeps dialogue logic separate from scene management and profile question logic.
 */
class GameDialogueManager(private val uiStateFlow: MutableStateFlow<GameUiState>) {
    private val dynamicReplyManager = DynamicReplyManager(uiStateFlow)

    /**
     * Generates character-specific dynamic reply based on affection, mood, intensity, and memory.
     */
    fun getDynamicReply(option: DialogueOption): String {
        return dynamicReplyManager.getDynamicReply(option)
    }

    /**
     * Determines player's interaction style based on accumulated memory points.
     * Returns "Playful", "Distant", or "Gentle" depending on dominant point type.
     */
    fun getPersonalityType(): String {
        val memory = uiStateFlow.value.memory

        return when {
            memory.playfulPoints > memory.gentlePoints &&
                    memory.playfulPoints > memory.distantPoints -> "Playful"

            memory.distantPoints > memory.gentlePoints &&
                    memory.distantPoints > memory.playfulPoints -> "Distant"

            else -> "Gentle"
        }
    }

    /**
     * Creates a personalized goodnight message based on scene, food, time, mood, and mood intensity.
     * Higher intensity makes the message more emotionally expressive.
     */
    fun getGoodnightMessage(): String {
        val state = uiStateFlow.value
        val memory = state.memory
        val mood = state.mood
        val moodIntensity = state.moodIntensity

        return when {
            memory.lastDateScene == "Cafe Date ☕" -> {
                if (moodIntensity >= 2) {
                    "Goodnight... I can't stop thinking about that quiet cafe with you."
                } else {
                    "Goodnight... I keep thinking about that quiet cafe with you."
                }
            }

            memory.lastDateScene == "Bingsu Date 🍧" -> {
                if (moodIntensity >= 2) {
                    "Goodnight... I'm still smiling about our bingsu date."
                } else {
                    "Goodnight... I keep thinking about our bingsu date."
                }
            }

            memory.favoriteFood.contains("Coffee", true) -> {
                if (moodIntensity >= 2) {
                    "Goodnight... I really want to sit somewhere quiet with you over coffee."
                } else {
                    "Goodnight... maybe next time we can sit somewhere quiet with coffee."
                }
            }

            memory.favoriteTime == "Rain 🌧️" ->
                "Goodnight... I like nights like this, soft and quiet."

            else -> when (mood) {
                "Happy" -> {
                    if (moodIntensity >= 2) {
                        "Goodnight... today felt amazing with you."
                    } else {
                        "Goodnight... today felt really nice."
                    }
                }

                "Shy" -> "Goodnight... I'll be here tomorrow."
                "Playful" -> {
                    if (moodIntensity >= 2) {
                        "Goodnight... don't forget about me, okay?"
                    } else {
                        "Goodnight... don't forget about me."
                    }
                }

                else -> "Goodnight... I'll be waiting."
            }
        }
    }

    /**
     * Character naturally recalls saved memories on a new day.
     * Memory intensity makes the recall feel fresher/more enthusiastic.
     */
    fun getMemoryRecallLine(): String? {
        val state = uiStateFlow.value
        val memory = state.memory
        val characterName = state.selectedCharacter?.name
        val personality = getPersonalityType()
        val moodIntensity = state.moodIntensity

        if (characterName == "Alice") {
            return when {
                memory.favoriteFood.contains("Coffee", ignoreCase = true) ->
                    "I remembered you like coffee... I thought that was very you."

                memory.favoriteTime == "Rain 🌧️" ->
                    "Rainy days feel quiet... I thought maybe you would like that too."

                memory.favoriteDate == "Cafe date" ->
                    "I was thinking about a quiet cafe... but I felt shy saying it first."

                else -> null
            }
        }

        return when {
            memory.lastDateScene == "Cafe Date ☕" -> {
                if (moodIntensity >= 2) {
                    "I can't stop replaying that quiet cafe... sitting with you felt so peaceful."
                } else {
                    "I was thinking about that quiet cafe... sitting with you felt peaceful."
                }
            }

            memory.lastDateScene == "Bingsu Date 🍧" -> {
                if (moodIntensity >= 2) {
                    "I keep reliving our bingsu date... it felt so much sweeter because you were there."
                } else {
                    "I keep thinking about our bingsu date... it felt sweeter because you were there."
                }
            }

            memory.favoriteFood.contains("Bingsu", ignoreCase = true) ->
                "You said you like bingsu... I still remember that."

            memory.favoriteFood.contains("Coffee", ignoreCase = true) ->
                "You said you like coffee... maybe that suits quiet nights like this."

            memory.favoriteTime == "Rain 🌧️" ->
                "Rainy nights always remind me of soft conversations."

            memory.favoriteTime == "Night 🌙" ->
                "Nights like this feel familiar somehow."

            personality == "Playful" ->
                "You have this playful side... I notice it more than you think."

            personality == "Distant" ->
                "You still feel a little hard to read... but I want to understand you."

            else -> null
        }
    }

    /**
     * Character sometimes initiates the conversation herself.
     * Intensity affects how eager/enthusiastic the initiated line seems.
     */
    fun getMinaInitiatedLine(): String? {
        val state = uiStateFlow.value
        val memory = state.memory
        val personality = getPersonalityType()
        val mood = state.mood
        val moodIntensity = state.moodIntensity

        return when {
            memory.lastDateScene == "Cafe Date ☕" -> {
                if (moodIntensity >= 2) {
                    "I can't stop thinking about that cafe... being there with you was so peaceful."
                } else {
                    "I was thinking about that cafe again... it felt peaceful being there with you."
                }
            }

            memory.lastDateScene == "Bingsu Date 🍧" -> {
                if (moodIntensity >= 2) {
                    "I keep smiling when I remember our bingsu date... it made my whole day brighter."
                } else {
                    "I still remember our bingsu date... it made me smile today."
                }
            }

            memory.favoriteFood.contains("Coffee", ignoreCase = true) ->
                "I saw a quiet cafe in my dream... it reminded me of you."

            memory.favoriteFood.contains("Bingsu", ignoreCase = true) ->
                "I thought about bingsu today... maybe because of you."

            memory.favoriteTime == "Rain 🌧️" ->
                "It feels like a rainy kind of night... soft and quiet."

            personality == "Playful" -> {
                if (moodIntensity >= 2) {
                    "You always make things fun... I was looking forward to tonight."
                } else {
                    "You know... you always make things a little more fun."
                }
            }

            personality == "Distant" ->
                "Sometimes I wonder what you're really thinking."

            mood == "Happy" -> {
                if (moodIntensity >= 2) {
                    "I was so sure you'd come back... I couldn't wait to see you."
                } else {
                    "I had a good feeling you would come back tonight."
                }
            }

            else -> null
        }
    }
}
