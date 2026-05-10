package com.example.dreambond

import com.example.dreambond.ui.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages all date scene logic: starting scenes and handling multi-step scene conversations.
 * This keeps scene logic separate from dialogue and core ViewModel logic.
 */
class GameSceneHandlers(private val uiStateFlow: MutableStateFlow<GameUiState>) {

    /**
     * Starts the Bingsu Date scene (single-step scene).
     */
    fun startBingsuDateScene() {
        uiStateFlow.update { current ->
            current.copy(
                activeScene = "BINGSU_DATE",
                currentMessage = "I remembered you like bingsu... do you want to go with me tonight?",
                latestResponse = "",
                sessionEnded = false,
                readyToEndDay = false,
                messages = current.messages + ChatMessage(
                    text = "I remembered you like bingsu... do you want to go with me tonight?",
                    isFromUser = false
                ),
                sceneOptions = listOf(
                    "Of course, let's go together.",
                    "Only if you choose the flavor.",
                    "Maybe next time."
                )
            )
        }
    }

    /**
     * Starts the Cafe Date scene (2-step: seating -> drink).
     */
    fun startCafeDateScene() {
        uiStateFlow.update { current ->
            current.copy(
                activeScene = "CAFE_DATE",
                sceneStep = 1,
                sessionEnded = false,
                messages = current.messages + ChatMessage(
                    text = "I remembered you like coffee... would you like to go to a quiet cafe with me?",
                    isFromUser = false
                ),
                sceneOptions = listOf(
                    "Let's sit by the window.",
                    "Let's sit in the quiet corner."
                )
            )
        }
    }

    /**
     * Starts the Tea House Date scene for Alice (2-step: acceptance -> tea selection).
     */
    fun startTeaHouseDateScene() {
        uiStateFlow.update { current ->
            current.copy(
                activeScene = "TEA_HOUSE_DATE",
                sceneStep = 1,
                sessionEnded = false,
                messages = current.messages + ChatMessage(
                    text = "There is a traditional tea house I wanted to show you... would you come with me?",
                    isFromUser = false
                ),
                sceneOptions = listOf(
                    "Of course, I'd love to.",
                    "What kind of tea do they serve?"
                )
            )
        }
    }

    /**
     * Starts the Bookstore Date scene for Alice (2-step: acceptance -> genre selection).
     */
    fun startBookstoreDateScene() {
        uiStateFlow.update { current ->
            current.copy(
                activeScene = "BOOKSTORE_DATE",
                sceneStep = 1,
                sessionEnded = false,
                messages = current.messages + ChatMessage(
                    text = "There is a quiet bookstore I love... would you like to go together?",
                    isFromUser = false
                ),
                sceneOptions = listOf(
                    "I'd love to see your favorite place.",
                    "What genre do you like to read?"
                )
            )
        }
    }

    /**
     * Routes scene choices to appropriate handler based on activeScene type.
     */
    fun chooseSceneOption(choice: String) {
        val state = uiStateFlow.value
        val currentScene = state.activeScene
        val currentStep = state.sceneStep

        // Route to multistep scene handlers when needed
        if (currentScene == "CAFE_DATE") {
            handleCafeDateChoice(choice, currentStep)
            return
        }

        if (currentScene == "TEA_HOUSE_DATE") {
            handleTeaHouseChoice(choice, currentStep)
            return
        }

        if (currentScene == "BOOKSTORE_DATE") {
            handleBookstoreDateChoice(choice, currentStep)
            return
        }

        // Handle single-step scenes (Bingsu)
        val reply = when (currentScene) {
            "BINGSU_DATE" -> when (choice) {
                "Of course, let's go together." ->
                    "Then let's share one. It feels sweeter with you."

                "Only if you choose the flavor." ->
                    "Then I'll choose strawberry. Don't complain later."

                "Maybe next time." ->
                    "Okay... maybe another night."

                else -> "..."
            }

            else -> "..."
        }

        val affectionGain = when (choice) {
            "Of course, let's go together." -> 4
            "Only if you choose the flavor." -> 3
            "Maybe next time." -> 0
            else -> 0
        }

        val sceneName = when (currentScene) {
            "BINGSU_DATE" -> "Bingsu Date 🍧"
            else -> ""
        }

        uiStateFlow.update { current ->
            current.copy(
                affection = current.affection + affectionGain,
                activeScene = "",
                sceneOptions = emptyList(),
                sessionEnded = true,
                readyToEndDay = false,
                memory = current.memory.copy(
                    lastDateScene = sceneName
                ),
                messages = current.messages +
                        ChatMessage(choice, isFromUser = true) +
                        ChatMessage(reply, isFromUser = false),
                latestResponse = reply,
                currentMessage = reply
            )
        }
    }

    /**
     * Handles Cafe Date as a 2-step scene:
     * Step 1: Choose where to sit
     * Step 2: Choose what to order
     */
    private fun handleCafeDateChoice(choice: String, step: Int) {
        when (step) {
            1 -> {
                val reply = when (choice) {
                    "Let's sit by the window." ->
                        "I like that... we can watch the city lights together."

                    "Let's sit in the quiet corner." ->
                        "That sounds peaceful... just us, away from everyone else."

                    else -> "..."
                }

                uiStateFlow.update { current ->
                    current.copy(
                        sceneStep = 2,
                        messages = current.messages +
                                ChatMessage(choice, isFromUser = true) +
                                ChatMessage(reply, isFromUser = false) +
                                ChatMessage(
                                    text = "What should we order?",
                                    isFromUser = false
                                ),
                        sceneOptions = listOf(
                            "Something sweet.",
                            "Something warm.",
                            "You choose for me."
                        )
                    )
                }
            }

            2 -> {
                val reply = when (choice) {
                    "Something sweet." ->
                        "Then maybe a sweet latte... it suits this moment."

                    "Something warm." ->
                        "Warm drinks make quiet nights feel softer."

                    "You choose for me." ->
                        "Then I'll choose something gentle for you."

                    else -> "..."
                }

                val affectionGain = when (choice) {
                    "Something sweet." -> 3
                    "Something warm." -> 3
                    "You choose for me." -> 4
                    else -> 1
                }

                uiStateFlow.update { current ->
                    current.copy(
                        affection = current.affection + affectionGain,
                        activeScene = "",
                        sceneStep = 0,
                        sceneOptions = emptyList(),
                        sessionEnded = true,
                        memory = current.memory.copy(
                            lastDateScene = "Cafe Date ☕"
                        ),
                        messages = current.messages +
                                ChatMessage(choice, isFromUser = true) +
                                ChatMessage(reply, isFromUser = false) +
                                ChatMessage(
                                    text = "I liked this cafe date... it felt calm being here with you.",
                                    isFromUser = false
                                ),
                        latestResponse = reply,
                        currentMessage = reply
                    )
                }
            }
        }
    }

    /**
     * Handles Tea House Date as a 2-step scene for Alice:
     * Step 1: Accept invitation
     * Step 2: Choose tea type
     */
    private fun handleTeaHouseChoice(choice: String, step: Int) {
        when (step) {
            1 -> {
                val reply = when (choice) {
                    "Of course, I'd love to." ->
                        "Really...? I'm so happy you want to go."

                    "What kind of tea do they serve?" ->
                        "They have many traditional varieties... I think you would like it there."

                    else -> "..."
                }

                uiStateFlow.update { current ->
                    current.copy(
                        sceneStep = 2,
                        messages = current.messages +
                                ChatMessage(choice, isFromUser = true) +
                                ChatMessage(reply, isFromUser = false) +
                                ChatMessage(
                                    text = "Which would you prefer... oolong or jasmine?",
                                    isFromUser = false
                                ),
                        sceneOptions = listOf(
                            "Oolong sounds nice.",
                            "Jasmine, definitely."
                        )
                    )
                }
            }

            2 -> {
                val reply = when (choice) {
                    "Oolong sounds nice." ->
                        "Good choice... it brings out the calm evening feeling."

                    "Jasmine, definitely." ->
                        "Jasmine is delicate and gentle... like you."

                    else -> "..."
                }

                val affectionGain = when (choice) {
                    "Oolong sounds nice." -> 3
                    "Jasmine, definitely." -> 4
                    else -> 1
                }

                uiStateFlow.update { current ->
                    current.copy(
                        affection = current.affection + affectionGain,
                        activeScene = "",
                        sceneStep = 0,
                        sceneOptions = emptyList(),
                        sessionEnded = true,
                        memory = current.memory.copy(
                            lastDateScene = "Tea House Date 🍵"
                        ),
                        messages = current.messages +
                                ChatMessage(choice, isFromUser = true) +
                                ChatMessage(reply, isFromUser = false) +
                                ChatMessage(
                                    text = "This tea house date... I'll remember it forever.",
                                    isFromUser = false
                                ),
                        latestResponse = reply,
                        currentMessage = reply
                    )
                }
            }
        }
    }

    /**
     * Handles Bookstore Date as a 2-step scene for Alice:
     * Step 1: Accept invitation
     * Step 2: Choose reading genre/activity
     */
    private fun handleBookstoreDateChoice(choice: String, step: Int) {
        when (step) {
            1 -> {
                val reply = when (choice) {
                    "I'd love to see your favorite place." ->
                        "Thank you... I was nervous showing you something so personal."

                    "What genre do you like to read?" ->
                        "I like many things... but poetry touches my heart the most."

                    else -> "..."
                }

                uiStateFlow.update { current ->
                    current.copy(
                        sceneStep = 2,
                        messages = current.messages +
                                ChatMessage(choice, isFromUser = true) +
                                ChatMessage(reply, isFromUser = false) +
                                ChatMessage(
                                    text = "Which section should we explore together...?",
                                    isFromUser = false
                                ),
                        sceneOptions = listOf(
                            "Let's find poetry together.",
                            "Show me your favorite book."
                        )
                    )
                }
            }

            2 -> {
                val reply = when (choice) {
                    "Let's find poetry together." ->
                        "You would read poetry... with me? That makes me so very happy."

                    "Show me your favorite book." ->
                        "You want to know my favorite...? It's about finding strength through love."

                    else -> "..."
                }

                val affectionGain = when (choice) {
                    "Let's find poetry together." -> 4
                    "Show me your favorite book." -> 3
                    else -> 1
                }

                uiStateFlow.update { current ->
                    current.copy(
                        affection = current.affection + affectionGain,
                        activeScene = "",
                        sceneStep = 0,
                        sceneOptions = emptyList(),
                        sessionEnded = true,
                        memory = current.memory.copy(
                            lastDateScene = "Bookstore Date 📚"
                        ),
                        messages = current.messages +
                                ChatMessage(choice, isFromUser = true) +
                                ChatMessage(reply, isFromUser = false) +
                                ChatMessage(
                                    text = "This bookstore date with you... I won't forget it.",
                                    isFromUser = false
                                ),
                        latestResponse = reply,
                        currentMessage = reply
                    )
                }
            }
        }
    }
}
