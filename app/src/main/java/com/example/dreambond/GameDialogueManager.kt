package com.example.dreambond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.MinaMemory
import com.example.dreambond.ui.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages dialogue generation and character-specific responses.
 * Handles dynamic replies, personality detection, memory recall lines, and goodnight messages.
 * This keeps dialogue logic separate from scene management and profile question logic.
 */
class GameDialogueManager(private val uiStateFlow: MutableStateFlow<GameUiState>) {

    /**
     * Generates character-specific dynamic reply based on affection, mood, intensity, and memory.
     * For Alice: Returns character-specific responses.
     * For Mina: Returns affection-level-based, mood-aware responses.
     */
    fun getDynamicReply(option: DialogueOption): String {
        val state = uiStateFlow.value
        val characterName = state.selectedCharacter?.name
        val mood = state.mood
        val moodIntensity = state.moodIntensity
        val affection = state.affection
        val lastChoice = state.memory.lastChoice
        val favoriteDate = state.memory.favoriteDate

        // Alice character branch (before Mina logic)
        if (characterName == "Alice") {
            return when (option.text) {
                "I wanted to see you." ->
                    "You say things like that so easily... it makes me shy."

                "I could not sleep." ->
                    "Then... if you want, I can stay here with you a little longer."

                "I was just curious." ->
                    "Curious about me...? I am not very interesting..."

                else ->
                    "I... I am still truing to understand you."
            }
        }

        // Mina character logic
        return when {
            affection < 10 -> {
                when (option.text) {
                    "I wanted to see you." -> {
                        if (lastChoice == "I could not sleep.") {
                            "You came back again... was it another restless night?"
                        } else {
                            "You wanted to see me... ? I didn't expect that."
                        }
                    }

                    "I could not sleep." -> "Then maybe the night brought you here for a reason."
                    else -> "You're a little hard to read... but I don't mind."
                }
            }

            affection < 25 -> {
                when (option.text) {
                    "I wanted to see you." -> {
                        when {
                            mood == "Happy" && moodIntensity >= 2 ->
                                "You came back... that really makes me happy."

                            mood == "Happy" ->
                                "I'm glad you came back."

                            mood == "Playful" ->
                                "You missed me that much?"

                            else ->
                                "I'm glad you came back tonight."
                        }
                    }

                    "I could not sleep." -> {
                        if (favoriteDate == "Night walk") {
                            "Maybe a quiet night walk would help you rest... you said you liked that."
                        } else {
                            "Then stay with me for a while. It's peaceful here."
                        }
                    }

                    else -> "You always say things that make me curious."
                }
            }

            affection < 50 -> {
                when (option.text) {
                    "I wanted to see you." -> {
                        if (moodIntensity >= 2) {
                            "I was hoping you'd say that... I really was."
                        } else {
                            "I was hoping you'd say that."
                        }
                    }

                    "I could not sleep." -> {
                        if (moodIntensity >= 2) {
                            "Then don't rush off yet... I like these quiet moments with you."
                        } else {
                            "Then don't rush off yet. I like these quiet moments with you."
                        }
                    }

                    "I was just curious." -> {
                        if (mood == "Playful" && moodIntensity >= 2) {
                            "Still curious about me? ...I find that cute."
                        } else {
                            "Still curious about me... I don't mind that at all."
                        }
                    }

                    else -> "You know... you're kind of cute when you act mysterious."
                }
            }

            else -> {
                when (option.text) {
                    "I wanted to see you." -> {
                        if (moodIntensity >= 2) {
                            "I missed you... I was waiting for you again. I always do."
                        } else {
                            "I missed you... I was waiting for you again."
                        }
                    }

                    "I could not sleep." -> {
                        if (moodIntensity >= 2) {
                            "Then stay. Nights feel softer when you're here... I don't want you to leave."
                        } else {
                            "Then stay. Nights feel softer when you're here."
                        }
                    }

                    "I was just curious." -> "You're here again... still curious about me?"
                    else -> {
                        if (moodIntensity >= 2) {
                            "Even when you pretend otherwise, you always come back to me. That makes me happy."
                        } else {
                            "Even when you pretend otherwise, you always come back to me."
                        }
                    }
                }
            }
        }
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

        // Alice character memory recall
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

        // Mina character memory recall
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

