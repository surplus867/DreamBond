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
// Responsible for chat logic, relationship progression, memory, scenes, and saving progress.
class GameViewModel(private val repository: GameRepository) : ViewModel() {

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

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

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
                    messages = listOf(ChatMessage(text = character.introLine, isFromUser = false))
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
    // Currently saves: character, affection, and day.
    // Memory is not persisted yet unless you add it to GameProgressEntity later.
    fun saveProgress() {
        val state = _uiState.value
        val character = state.selectedCharacter ?: return

        val progress = GameProgressEntity(
            id = character.id,
            selectedCharacter = character.name,
            affection = state.affection,
            day = state.day
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
    // moodIntensity varies from 0 to 3, with higher values making replies more emotionally expressive.
    fun getDynamicReply(option: DialogueOption): String {
        val mood = _uiState.value.mood
        val moodIntensity = _uiState.value.moodIntensity
        val affection = _uiState.value.affection
        val lastChoice = _uiState.value.memory.lastChoice
        val favoriteDate = _uiState.value.memory.favoriteDate

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

    // Determines the player's interaction style based on memory points.
    fun getPersonalityType(): String {
        val memory = _uiState.value.memory

        return when {
            memory.playfulPoints > memory.gentlePoints &&
                    memory.playfulPoints > memory.distantPoints -> "Playful"

            memory.distantPoints > memory.gentlePoints &&
                    memory.distantPoints > memory.playfulPoints -> "Distant"

            else -> "Gentle"
        }
    }

    // Creates a personalized goodnight message based on scene, food, time, mood, and moodIntensity.
    // Higher intensity makes the message more emotionally expressive.
    fun getGoodnightMessage(): String {
        val memory = _uiState.value.memory
        val mood = _uiState.value.mood
        val moodIntensity = _uiState.value.moodIntensity

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
                foodOptions = listOf("Bingsu 🍧", "Coffee ☕", "Cake 🍰"),
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
    fun startBingsuDateScene() {
        _uiState.update { current ->
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

    // Starts the Cafe Date scene.
    // This is a multistep scene using sceneStep.
    fun startCafeDateScene() {
        _uiState.update { current ->
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

    // Handles scene choices.
    // Routes Cafe Date to its own multistep handler.
    fun chooseSceneOption(choice: String) {
        val currentScene = _uiState.value.activeScene
        val currentStep = _uiState.value.sceneStep

        // Route to a multistep scene handler when needed.
        if (currentScene == "CAFE_DATE") {
            handleCafeDateChoice(choice, currentStep)
            return
        }

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

        _uiState.update { current ->
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

    // Handles Cafe Date as a 2-step mini story:
    // Step 1: choose seat
    // Step 2: choose drink
    private fun handleCafeDateChoice(choice: String, step: Int) {
        // Simple 2-step scene state machine: seating choice -> order choice -> finish.
        when (step) {
            1 -> {
                val reply = when (choice) {
                    "Let's sit by the window." ->
                        "I like that... we can watch the city lights together."

                    "Let's sit in the quiet corner." ->
                        "That sounds peaceful... just us, away from everyone else."

                    else -> "..."
                }

                _uiState.update { current ->
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

                _uiState.update { current ->
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

    // Mina recalls saved memories naturally on a new day, with intensity making them feel more fresh.
    fun getMemoryRecallLine(): String? {
        val memory = _uiState.value.memory
        val personality = getPersonalityType()
        val moodIntensity = _uiState.value.moodIntensity

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

    // Mina sometimes starts the conversation herself, with intensity affecting how eager she seems.
    fun getMinaInitiatedLine(): String? {
        val memory = _uiState.value.memory
        val personality = getPersonalityType()
        val mood = _uiState.value.mood
        val moodIntensity = _uiState.value.moodIntensity

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