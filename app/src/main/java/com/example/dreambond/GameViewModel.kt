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
import kotlin.time.Duration.Companion.milliseconds

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
    val sceneOptions: List<String> = emptyList(),
    val conversationTurns: Int = 0,
    val currentContextTag: String = "",
    val currentContextTurns: Int = 0
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
    private val conversationFlowManager = ConversationFlowManager(_uiState)
    private val sessionFlowManager = GameSessionFlowManager(_uiState, sceneHandlers)
    private val backgroundContextDetector = ConversationContextDetector()

    // Converts the user's selected reply into Mina's current mood.
    // This mood affects future dialogue and goodnight messages.
    private fun getMoodFromOption(option: DialogueOption): String {
        return when {
            option.affectionChange >= 2 -> "Happy"
            option.affectionChange == 1 -> "Calm"
            else -> "Playful"
        }
    }

    private fun normalizedChoiceText(option: DialogueOption): String {
        return when {
            option.affectionChange >= 2 -> "I wanted to see you."
            option.affectionChange == 1 -> "I could not sleep."
            else -> "I was just curious."
        }
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

    val dialogueOptions: List<DialogueOption>
        get() = conversationFlowManager.getDialogueOptions()


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
            val normalizedChoice = normalizedChoiceText(option)
            val updatedMemory = when {
                option.affectionChange >= 2 -> current.memory.copy(
                    lastChoice = normalizedChoice,
                    gentlePoints = current.memory.gentlePoints + 1
                )

                option.affectionChange == 1 -> current.memory.copy(
                    lastChoice = normalizedChoice,
                    gentlePoints = current.memory.gentlePoints + 1
                )

                else -> current.memory.copy(
                    lastChoice = normalizedChoice,
                    distantPoints = current.memory.distantPoints + 1
                )
            }

            current.copy(
                isTyping = true,
                sessionEnded = false,
                readyToEndDay = false,
                mood = getMoodFromOption(option),
                moodIntensity = 3,
                messages = current.messages + ChatMessage(text = option.text, isFromUser = true),
                memory = updatedMemory
            )
        }

        // Phase 2: after a short delay, post Mina's response and end this turn.
        viewModelScope.launch {
            delay(1200.milliseconds)
            _uiState.update { current ->
                val countedTurns = current.messages.count { message ->
                    message.isFromUser && conversationFlowManager.isDialogueOptionText(message.text)
                }
                val updatedTurns = maxOf(current.conversationTurns + 1, countedTurns)
                val shouldEndSession = updatedTurns >= 4
                val detectedContextTag = conversationFlowManager.detectContextTagFromText(
                    listOf(dynamicReply)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                )
                val (nextContextTag, nextContextTurns) = when {
                    shouldEndSession -> {
                        val stableContext = if (detectedContextTag.isNotBlank()) {
                            detectedContextTag
                        } else {
                            current.currentContextTag
                        }
                        stableContext to 0
                    }
                    detectedContextTag.isNotBlank() && detectedContextTag != current.currentContextTag ->
                        detectedContextTag to 1
                    detectedContextTag.isNotBlank() && detectedContextTag == current.currentContextTag -> {
                        val updatedContextTurns = current.currentContextTurns + 1
                        if (updatedContextTurns >= 2) {
                            "" to 0
                        } else {
                            detectedContextTag to updatedContextTurns
                        }
                    }
                    detectedContextTag.isBlank() && current.currentContextTag.isNotBlank() -> {
                        val updatedContextTurns = current.currentContextTurns + 1
                        if (updatedContextTurns >= 2) {
                            "" to 0
                        } else {
                            current.currentContextTag to updatedContextTurns
                        }
                    }
                    else -> "" to 0
                }
                val replyMessages = listOf(
                    ChatMessage(text = dynamicReply, isFromUser = false)
                )

                current.copy(
                    affection = current.affection + option.affectionChange,
                    currentMessage = dynamicReply,
                    latestResponse = dynamicReply,
                    sessionEnded = shouldEndSession,
                    readyToEndDay = false,
                    isTyping = false,
                    conversationTurns = updatedTurns,
                    activeScene = "",
                    sceneOptions = emptyList(),
                    currentContextTag = nextContextTag,
                    currentContextTurns = nextContextTurns,
                    messages = current.messages + replyMessages
                )
            }
        }
    }

    fun nextDay() {
        sessionFlowManager.nextDay(
            getMinaInitiatedLine = dialogueManager::getMinaInitiatedLine,
            getMemoryRecallLine = dialogueManager::getMemoryRecallLine
        )
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

    fun askFavoriteDateQuestion(intro: String? = null) {
        sessionFlowManager.askFavoriteDateQuestion(intro)
    }

    fun selectFavoriteDate(date: String) {
        sessionFlowManager.selectFavoriteDate(date)
    }

    fun askFavoriteFoodQuestion(intro: String? = null) {
        sessionFlowManager.askFavoriteFoodQuestion(intro)
    }

    fun selectFavoriteFood(food: String) {
        sessionFlowManager.selectFavoriteFood(food)
    }

    fun askFavoriteTimeQuestion(intro: String? = null) {
        sessionFlowManager.askFavoriteTimeQuestion(intro)
    }

    fun selectFavoriteTime(time: String) {
        sessionFlowManager.selectFavoriteTime(time)
    }

    // Starts the Bingsu Date scene.
// Delegates to GameSceneHandlers.
    fun startBingsuDateScene() {
        sceneHandlers.startBingsuDateScene()
    }

    // Starts the cafè latte scene (2-step).
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

    fun getBackgroundRes(
        characterName: String?,
        activeScene: String,
        contextTag: String = "",
        latestResponse: String = "",
        currentMessage: String = "",
        messages: List<ChatMessage> = emptyList()
    ): Int {
        val effectiveContextTag = if (contextTag.isNotBlank()) {
            contextTag
        } else {
            val currentLineContext = backgroundContextDetector.detect("$latestResponse $currentMessage")
            currentLineContext.ifBlank {
                messages
                    .asReversed()
                    .asSequence()
                    .map { message -> backgroundContextDetector.detect(message.text) }
                    .firstOrNull { detected -> detected.isNotBlank() }
                    .orEmpty()
            }
        }
        return when (activeScene) {
            "CAFE_DATE", "BINGSU_DATE" -> R.drawable.cafe_bg
            "TEA_DATE", "TEA_HOUSE_DATE" -> R.drawable.bg_tea_house
            "BOOKSTORE_DATE" -> R.drawable.bg_bookstore
            "UNIVERSITY_DATE" -> R.drawable.bg_university
            "PARK_DATE" -> R.drawable.bg_park
            "FESTIVAL_DATE" -> R.drawable.bg_festival
            else -> when (effectiveContextTag) {
                "bookstore" -> R.drawable.bg_bookstore
                "coffee", "bingsu", "bingsu_flavor" -> R.drawable.cafe_bg
                "rain" -> R.drawable.rain_bg
                "movie" -> R.drawable.bg_festival
                "night_walk" -> R.drawable.bg_park
                else -> R.drawable.bg_default_night
            }
        }
    }
}
