package com.example.dreambond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreambond.data.GameRepository
import com.example.dreambond.data.local.GameProgressEntity
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.GirlfriendCharacter
import com.example.dreambond.model.MinaMemory
import com.example.dreambond.ui.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// Holds all UI/game state for the current play session.
// Compose observes this through StateFlow and updates the UI automatically.
data class GameUiState(
    val selectedCharacter: GirlfriendCharacter? = null,
    val affection: Int = 0,
    val sceneStep: Int = 0,
    val currentMessage: String = "",
    val latestResponse: String = "",
    val day: Int = 1,
    val sessionEnded: Boolean = false,
    val readyToEndDay: Boolean = false,
    val isTyping: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val memory: MinaMemory = MinaMemory(),
    val mood: String = "Calm",
    val moodIntensity: Int = 0,
    val showDateQuestion: Boolean = false,
    val dateOptions: List<String> = emptyList(),
    val showFoodQuestion: Boolean = false,
    val foodOptions: List<String> = emptyList(),
    val showTimeQuestion: Boolean = false,
    val timeOptions: List<String> = emptyList(),
    val activeScene: String = "",
    val sceneOptions: List<String> = emptyList()
)

// Main ViewModel for DreamBond.
// Responsible for core chat logic, relationship progression, profile questions, and saving progress.
// Delegates dialogue generation to GameDialogueManager and date scenes to GameSceneHandlers.
class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Delegate managers for separated concerns
    private val dialogueManager = GameDialogueManager(_uiState)
    private val sceneHandlers = GameSceneHandlers(_uiState)

    // Converts the user's selected reply into Mina's current mood.
    // This mood affects future dialogue and goodnight messages.
    private fun getMoodFromChoice(choice: String): String {
        return when (choice) {
            "I wanted to see you." -> "Happy"
            "I could not sleep." -> "Calm"
            "I was just curious." -> "Playful"
            else -> "Calm"
        }
    }

    // Decays mood intensity over time as each day passes.
    // When intensity reaches 0, resets mood to "Calm".
    // Returns a Pair of (newMood, newIntensity).
    private fun decayMood(currentMood: String, currentIntensity: Int): Pair<String, Int> {
        val newIntensity = (currentIntensity - 1).coerceAtLeast(0)
        val newMood = if (newIntensity == 0) "Calm" else currentMood
        return newMood to newIntensity
    }

    val characters = listOf(
        GirlfriendCharacter(
            id = 1,
            name = "Mina",
            personality = "Calm and caring",
            introLine = "You came back tonight. I was waiting for you."
        ),

        GirlfriendCharacter(
            id = 2,
            name = "Alice",
            personality = "Shy and traditional",
            introLine = "Oh... you came. I was not sure if you would."
        )
    )

    val dialogueOptions = listOf(
        DialogueOption(
            text = "I wanted to see you.",
            affectionChange = 3,
            response = "That makes me happy... I hoped you would say that."
        ),
        DialogueOption(
            text = "I could not sleep.",
            affectionChange = 1,
            response = "Then stay with me a little longer tonight."
        ),
        DialogueOption(
            text = "I was just curious.",
            affectionChange = 0,
            response = "Curious? That is still a reason to come back, I guess."
        )
    )


    // Selects Mina and restores saved progress if it exists.
    // If there is no saved progress, starts a fresh conversation.
    fun selectCharacter(character: GirlfriendCharacter) {
        viewModelScope.launch {
            val savedProgress = repository.getProgress(character.id)
            if (savedProgress != null) {
                _uiState.value = GameUiState(
                    selectedCharacter = character,
                    affection = savedProgress.affection,
                    currentMessage = character.introLine,
                    latestResponse = "",
                    day = savedProgress.day,
                    sessionEnded = false,
                    isTyping = false,
                    messages = listOf(ChatMessage(text = character.introLine, isFromUser = false)),
                    memory = MinaMemory(
                        favoriteDate = savedProgress.favoriteDate,
                        favoriteFood = savedProgress.favoriteFood,
                        favoriteTime = savedProgress.favoriteTime,
                        lastDateScene = savedProgress.lastDateScene,
                        gentlePoints = savedProgress.gentlePoints,
                        playfulPoints = savedProgress.playfulPoints,
                        distantPoints = savedProgress.distantPoints
                    ),
                    mood = savedProgress.mood,
                    moodIntensity = savedProgress.moodIntensity
                )
            } else {
                _uiState.value = GameUiState(
                    selectedCharacter = character,
                    affection = 0,
                    currentMessage = character.introLine,
                    latestResponse = "",
                    day = 1,
                    sessionEnded = false,
                    isTyping = false,
                    messages = listOf(ChatMessage(text = character.introLine, isFromUser = false))
                )
            }
        }
    }

    // Handles a normal chat reply.
    // Phase 1: user message appears and Mina starts typing,
    // Phase 2: after delay, Mina replies and the turn ends.
    fun chooseReply(option: DialogueOption) {
        val dynamicReply = getDynamicReply(option)

        // Phase 1: immediately show the player choice and set Mina to typing.
        _uiState.update { current ->
            val updatedMemory = when (option.text) {
                "I wanted to see you." -> current.memory.copy(
                    lastChoice = option.text,
                    gentlePoints = current.memory.gentlePoints + 1
                )

                "I could not sleep." -> current.memory.copy(
                    lastChoice = option.text,
                    gentlePoints = current.memory.gentlePoints + 1
                )

                "I was just curious." -> current.memory.copy(
                    lastChoice = option.text,
                    distantPoints = current.memory.distantPoints + 1
                )

                else -> current.memory.copy(
                    lastChoice = option.text
                )
            }

            current.copy(
                isTyping = true,
                sessionEnded = false,
                readyToEndDay = false,
                mood = getMoodFromChoice(option.text),
                moodIntensity = 3,
                messages = current.messages + ChatMessage(text = option.text, isFromUser = true),
                memory = updatedMemory
            )
        }

        // Phase 2: after a short delay, post Mina's response and end this turn.
        viewModelScope.launch {
            delay(1200)
            _uiState.update { current ->
                current.copy(
                    affection = current.affection + option.affectionChange,
                    currentMessage = dynamicReply,
                    latestResponse = dynamicReply,
                    sessionEnded = true,
                    readyToEndDay = false,
                    isTyping = false,
                    messages = current.messages + ChatMessage(
                        text = dynamicReply,
                        isFromUser = false
                    )
                )
            }
        }
    }

    // Start the next day.
    // Priority:
    /// 1. Ask missing memory questions.
    /// 2. Let Mina initiate conversation.
    /// 3. Recall past memory
    /// 4. Use default intro
    fun nextDay() {
        val character = _uiState.value.selectedCharacter
        val memory = _uiState.value.memory

        // Ask profile questions in order across different days.
        val shouldAskDate = memory.favoriteDate.isBlank()
        val shouldAskFood = memory.favoriteDate.isNotBlank() && memory.favoriteFood.isBlank()
        val shouldAskTime = memory.favoriteFood.isNotBlank() && memory.favoriteTime.isBlank()

        val currentIntensity = _uiState.value.moodIntensity
        val (newMood, newIntensity) = decayMood(_uiState.value.mood, currentIntensity)

        val defaultIntro = when {
            memory.lastChoice == "I wanted to see you." -> {
                if (currentIntensity >= 2) {
                    "You came back tonight... I was really hoping you would."
                } else {
                    "You came back tonight... I was hoping you would."
                }
            }

            memory.lastChoice == "I could not sleep." ->
                "Another quiet night... are you having trouble sleeping again?"

            memory.lastChoice == "I was just curious." ->
                "You're here again... still curious about me?"

            shouldAskTime ->
                "I keep thinking about what you told me... can I ask you one more thing?"

            shouldAskFood ->
                "I've been curious about you more and more... is it okay if I ask something?"

            memory.favoriteDate.isBlank() ->
                "Before tonight begins... can I ask you something?"

            else ->
                character?.introLine ?: ""
        }

        val initiatedLine = getMinaInitiatedLine()
        val recallLine = getMemoryRecallLine()

        // Priority: required questions first; otherwise mix variation lines with weighted randomness.
        val intro = when {
            shouldAskDate || shouldAskFood || shouldAskTime -> defaultIntro
            initiatedLine != null && Random.nextInt(100) < 60 -> initiatedLine
            recallLine != null && Random.nextInt(100) < 40 -> recallLine
            else -> defaultIntro
        }

        if (shouldAskDate) {
            askFavoriteDateQuestion(intro)
            return
        }
        if (shouldAskFood) {
            askFavoriteFoodQuestion(intro)
            return
        }
        if (shouldAskTime) {
            askFavoriteTimeQuestion(intro)
            return
        }

        _uiState.update { current ->
            current.copy(
                day = current.day + 1,
                currentMessage = intro,
                latestResponse = "",
                sessionEnded = false,
                isTyping = false,
                messages = listOf(ChatMessage(text = intro, isFromUser = false)),
                showDateQuestion = false,
                dateOptions = emptyList(),
                showFoodQuestion = false,
                foodOptions = emptyList(),
                showTimeQuestion = false,
                timeOptions = emptyList(),
                mood = newMood,
                moodIntensity = newIntensity
            )
        }
    }

    // Saves only basic progress to Room.
    // Currently, saves: character, affection, and day.
    // Memory is not persisted yet unless you add it to GameProgressEntity later.
    fun saveProgress() {
        val state = _uiState.value
        val character = state.selectedCharacter ?: return

        val progress = GameProgressEntity(

            id = character.id,

            selectedCharacter = character.name,

            affection = state.affection,

            day = state.day,

            favoriteDate = state.memory.favoriteDate,

            favoriteFood = state.memory.favoriteFood,

            favoriteTime = state.memory.favoriteTime,

            lastDateScene = state.memory.lastDateScene,

            playfulPoints = state.memory.gentlePoints,

            distantPoints = state.memory.distantPoints,

            mood = state.mood,

            moodIntensity = state.moodIntensity
        )

        viewModelScope.launch {
            repository.saveProgress(progress)
        }
    }

    fun resetGame() {
        clearAllProgress()
    }

    fun clearAllProgress() {
        viewModelScope.launch {
            repository.clearAllProgress()
            _uiState.value = GameUiState()
        }
    }

    // Converts affection points into relationship level.
    fun getRelationShipLevel(): String {
        val affection = _uiState.value.affection
        return when {
            affection < 10 -> "Stranger"
            affection < 20 -> "Friend"
            affection < 50 -> "Close"
            else -> "Special"
        }
    }

    // Generates Mina's reply based on affection, mood, moodIntensity, and memory.
    // Delegates to GameDialogueManager for complex character-specific logic.
    fun getDynamicReply(option: DialogueOption): String {
        return dialogueManager.getDynamicReply(option)
    }

    // Determines the player's interaction style based on memory points.
    // Delegates to GameDialogueManager.
    fun getPersonalityType(): String {
        return dialogueManager.getPersonalityType()
    }

    // Creates a personalized goodnight message based on scene, food, time, mood, and moodIntensity.
    // Delegates to GameDialogueManager.
    fun getGoodnightMessage(): String {
        return dialogueManager.getGoodnightMessage()
    }

    // Character recalls saved memories naturally on a new day.
    // Delegates to GameDialogueManager.
    fun getMemoryRecallLine(): String? {
        return dialogueManager.getMemoryRecallLine()
    }

    // Character sometimes initiates conversation with player-dependent lines.
    // Delegates to GameDialogueManager.
    fun getMinaInitiatedLine(): String? {
        return dialogueManager.getMinaInitiatedLine()
    }

    // Starts a profile question: favorite date type.
    fun askFavoriteDateQuestion(intro: String? = null) {
        _uiState.update { current ->
            val dayIntro = intro ?: current.currentMessage
            val baseMessages = if (intro != null) {
                listOf(ChatMessage(text = dayIntro, isFromUser = false))
            } else {
                current.messages
            }
            current.copy(
                day = if (intro != null) current.day + 1 else current.day,
                currentMessage = dayIntro,
                latestResponse = if (intro != null) "" else current.latestResponse,
                sessionEnded = if (intro != null) false else current.sessionEnded,
                isTyping = if (intro != null) false else current.isTyping,
                showDateQuestion = true,
                dateOptions = listOf("Night walk", "Cafe date", "Movie night"),
                showFoodQuestion = false,
                foodOptions = emptyList(),
                showTimeQuestion = false,
                timeOptions = emptyList(),
                messages = baseMessages + ChatMessage(
                    text = "What kind of date would you like with me?",
                    isFromUser = false
                )
            )
        }
    }

    // Saves user's favorite date choice into Mina's memory.
    fun selectFavoriteDate(date: String) {
        val response = "Ahh... $date sounds nice. I'll remember that."
        _uiState.update { current ->
            current.copy(
                showDateQuestion = false,
                dateOptions = emptyList(),
                currentMessage = response,
                sessionEnded = true,
                readyToEndDay = false,
                latestResponse = response,
                memory = current.memory.copy(favoriteDate = date),
                messages = current.messages +
                        ChatMessage(text = date, isFromUser = true) +
                        ChatMessage(text = response, isFromUser = false)
            )
        }
    }

    // Saves favorite food.
    // Some answers can trigger special date scenes.
    fun askFavoriteFoodQuestion(intro: String? = null) {
        _uiState.update { current ->
            val dayIntro = intro ?: current.currentMessage
            val baseMessages = if (intro != null) {
                listOf(ChatMessage(text = dayIntro, isFromUser = false))
            } else {
                current.messages
            }
            current.copy(
                day = if (intro != null) current.day + 1 else current.day,
                currentMessage = dayIntro,
                latestResponse = if (intro != null) "" else current.latestResponse,
                sessionEnded = if (intro != null) false else current.sessionEnded,
                isTyping = if (intro != null) false else current.isTyping,
                showFoodQuestion = true,
                foodOptions = listOf("Bingsu 🍧", "Coffee ☕", "Cake 🍰", "Tea 🍵"),
                showDateQuestion = false,
                dateOptions = emptyList(),
                showTimeQuestion = false,
                timeOptions = emptyList(),
                messages = baseMessages + ChatMessage(
                    text = "What kind of dessert do you like?",
                    isFromUser = false
                )
            )
        }
    }

    // Saves favorite food.
    // Bingsu starts Bingsu Date.
    // Coffee starts Cafe Date.
    fun selectFavoriteFood(food: String) {
        // Some food answers branch directly into a short date scene instead of ending the day.
        if (food.contains("Bingsu", ignoreCase = true)) {
            _uiState.update { current ->
                current.copy(
                    showFoodQuestion = false,
                    foodOptions = emptyList(),
                    readyToEndDay = false,
                    memory = current.memory.copy(favoriteFood = food),
                    messages = current.messages +
                            ChatMessage(text = food, isFromUser = true) +
                            ChatMessage(
                                text = "Ahh... $food sounds nice. I'll remember that.",
                                isFromUser = false
                            )
                )
            }
            startBingsuDateScene()
            return
        }

        if (food.contains("Coffee", ignoreCase = true)) {
            _uiState.update { current ->
                current.copy(
                    showFoodQuestion = false,
                    foodOptions = emptyList(),
                    readyToEndDay = false,
                    memory = current.memory.copy(favoriteFood = food),
                    messages = current.messages +
                            ChatMessage(text = food, isFromUser = true) +
                            ChatMessage(
                                text = "Ahh... $food sounds nice. I'll remember that.",
                                isFromUser = false
                            )
                )
            }
            startCafeDateScene()
            return
        }

        // Tea triggers Tea House date scene for Alice, or normal ending for Mina
        if (food.contains("Tea", ignoreCase = true)) {
            val characterName = _uiState.value.selectedCharacter?.name

            // Alice gets Tea House scene
            if (characterName == "Alice") {
                _uiState.update { current ->
                    current.copy(
                        showFoodQuestion = false,
                        foodOptions = emptyList(),
                        readyToEndDay = false,
                        memory = current.memory.copy(favoriteFood = food),
                        messages = current.messages +
                                ChatMessage(text = food, isFromUser = true) +
                                ChatMessage(
                                    text = "Ahh... $food sounds nice. I'll remember that.",
                                    isFromUser = false
                                )
                    )
                }
                startTeaHouseDateScene()
                return
            }

            // Mina gets normal ending with sessionEnded = true
            val response = "Ahh... $food sounds nice. I'll remember that."
            _uiState.update { current ->
                current.copy(
                    showFoodQuestion = false,
                    foodOptions = emptyList(),
                    currentMessage = response,
                    latestResponse = response,
                    sessionEnded = true,
                    readyToEndDay = false,
                    memory = current.memory.copy(favoriteFood = food),
                    messages = current.messages +
                            ChatMessage(text = food, isFromUser = true) +
                            ChatMessage(text = response, isFromUser = false)
                )
            }
            return
        }

        val response = "Ahh... $food sounds nice. I'll remember that."
        _uiState.update { current ->
            current.copy(
                showFoodQuestion = false,
                foodOptions = emptyList(),
                currentMessage = response,
                latestResponse = response,
                sessionEnded = true,
                readyToEndDay = false,
                memory = current.memory.copy(favoriteFood = food),
                messages = current.messages +
                        ChatMessage(text = food, isFromUser = true) +
                        ChatMessage(text = response, isFromUser = false)
            )
        }
    }

    // Starts the "favorite time of day" question (Night / Rain / Sunset).
    // Optionally replaces the current into when triggered at the start of a new day.
    // Updates UI to show time options and adds Mina's question messages.
    fun askFavoriteTimeQuestion(intro: String? = null) {
        _uiState.update { current ->
            val dayIntro = intro ?: current.currentMessage
            val baseMessages = if (intro != null) {
                listOf(ChatMessage(text = dayIntro, isFromUser = false))
            } else {
                current.messages
            }
            current.copy(
                day = if (intro != null) current.day + 1 else current.day,
                currentMessage = dayIntro,
                latestResponse = if (intro != null) "" else current.latestResponse,
                sessionEnded = if (intro != null) false else current.sessionEnded,
                isTyping = if (intro != null) false else current.isTyping,
                showTimeQuestion = true,
                timeOptions = listOf("Night 🌙", "Rain 🌧️", "Sunset 🌆"),
                showDateQuestion = false,
                dateOptions = emptyList(),
                showFoodQuestion = false,
                foodOptions = emptyList(),
                messages = baseMessages + ChatMessage(
                    text = "What is your favorite time of day?",
                    isFromUser = false
                )
            )
        }
    }

    // Handles user's selection of favorite time preference.
    // Updates memory (favoriteTime), generates a response, and ends the current interaction.
    // This value is later used for:
    // - dynamic replies
    // - memory recall lines
    // - goodnight messages
    fun selectFavoriteTime(time: String) {
        val characterName = _uiState.value.selectedCharacter?.name

        // Alice + Rain triggers the Bookstore Date scene
        if (characterName == "Alice" && time.contains("Rain", ignoreCase = true)) {
            _uiState.update { current ->
                current.copy(
                    showTimeQuestion = false,
                    timeOptions = emptyList(),
                    readyToEndDay = false,
                    memory = current.memory.copy(
                        favoriteTime = time
                    ),
                    messages = current.messages +
                            ChatMessage(text = time, isFromUser = true) +
                            ChatMessage(
                                text = "Rainy days... there is something I want to show you on a day like this.",
                                isFromUser = false
                            )
                )
            }
            startBookstoreDateScene()
            return
        }

        val response = "Ahh… $time. That tells me a lot about you. I'll keep that close."
        _uiState.update { current ->
            current.copy(
                showTimeQuestion = false,
                timeOptions = emptyList(),
                currentMessage = response,
                sessionEnded = true,
                readyToEndDay = false,
                latestResponse = response,
                memory = current.memory.copy(
                    favoriteTime = time
                ),
                messages = current.messages +
                        ChatMessage(text = time, isFromUser = true) +
                        ChatMessage(text = response, isFromUser = false)
            )
        }
    }

    // Starts the Bingsu Date scene.
    // Delegates to GameSceneHandlers.
    fun startBingsuDateScene() {
        sceneHandlers.startBingsuDateScene()
    }

    // Starts the Cafe Date scene (2-step).
    // Delegates to GameSceneHandlers.
    fun startCafeDateScene() {
        sceneHandlers.startCafeDateScene()
    }

    // Starts the Tea House Date scene for Alice (2-step).
    // Delegates to GameSceneHandlers.
    fun startTeaHouseDateScene() {
        sceneHandlers.startTeaHouseDateScene()
    }

    // Starts the Bookstore Date scene for Alice (2-step).
    // Delegates to GameSceneHandlers.
    fun startBookstoreDateScene() {
        sceneHandlers.startBookstoreDateScene()
    }

    // Handles scene choices, routing to appropriate scene handler.
    // Delegates to GameSceneHandlers.
    fun chooseSceneOption(choice: String) {
        sceneHandlers.chooseSceneOption(choice)
    }

    // Adds Mina's final goodnight line before allowing the user to end the day.
    fun continueAfterReply() {
        val finalLine = getGoodnightMessage()

        _uiState.update { current ->
            // Only append the goodnight line once, and only after a turn has ended.
            if (!current.sessionEnded || current.readyToEndDay) {
                return@update current
            }

            current.copy(
                readyToEndDay = true,
                latestResponse = finalLine,
                currentMessage = finalLine,
                messages = current.messages + ChatMessage(
                    text = finalLine,
                    isFromUser = false
                )
            )
        }
    }
}