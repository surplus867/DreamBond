package com.example.dreambond.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dreambond.ChatBubble
import com.example.dreambond.GameViewModel
import com.example.dreambond.R
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.GirlfriendCharacter
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(
    viewModel: GameViewModel,
    character: GirlfriendCharacter?,
    affection: Int,
    relationshipLevel: String,
    personalityType: String,
    mood: String,
    currentMessage: String,
    latestResponse: String,
    sessionEnded: Boolean,
    readyToEndDay: Boolean,
    isTyping: Boolean,
    messages: List<ChatMessage>,
    options: List<DialogueOption>,
    showDateQuestion: Boolean,
    dateOptions: List<String>,
    showFoodQuestion: Boolean,
    foodOptions: List<String>,
    showTimeQuestion: Boolean,
    timeOptions: List<String>,
    activeScene: String,
    sceneOptions: List<String>,
    isMusicEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onChooseReply: (DialogueOption) -> Unit,
    onSelectFavoriteDate: (String) -> Unit,
    onSelectFavoriteFood: (String) -> Unit,
    onSelectFavoriteTime: (String) -> Unit,
    onChooseSceneOption: (String) -> Unit,
    onContinueAfterReply: () -> Unit,
    onEndDay: () -> Unit,
    onSpeakLatestResponse: (String) -> Unit
) {
    var displayText by remember { mutableStateOf("") }
    val typingSpeed = 25L

    val backgroundRes = viewModel.getBackgroundRes(character?.name, activeScene)

    LaunchedEffect(latestResponse, isTyping, sessionEnded) {
        if (sessionEnded && latestResponse.isNotBlank() && !isTyping) {
            delay(300)
            onSpeakLatestResponse(latestResponse)
        }
    }

    val visibleMessages = messages.filter { it.text.isNotBlank() }
    val visibleCurrentMessage = currentMessage.trim()

    LaunchedEffect(visibleCurrentMessage) {
        if (visibleMessages.isEmpty() && visibleCurrentMessage.isNotEmpty()) {
            displayText = ""
            visibleCurrentMessage.forEach { char ->
                displayText += char
                delay(typingSpeed)
            }
        }
    }

    val statusColor = when (relationshipLevel) {
        "Stranger" -> Color.Gray
        "Friend" -> Color(0xFF4CAF50)
        "Close" -> Color(0xFF2196F3)
        else -> Color(0xFFE91E63)
    }

    val portraitRes = when (character?.name) {
        "Mina" -> when (relationshipLevel) {
            "Stranger" -> R.drawable.mina_stranger
            "Friend" -> R.drawable.mina_friend
            "Close" -> R.drawable.mina_close
            else -> R.drawable.mina_special
        }

        "Alice" -> R.drawable.alice_default
        else -> R.drawable.mina_special
    }

    val shouldShowMessageContainer =
        !sessionEnded && (visibleMessages.isNotEmpty() || visibleCurrentMessage.isNotEmpty() || isTyping)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            if (sessionEnded && !isTyping && !readyToEndDay) {
                Surface(color = Color.Transparent) {
                    Button(
                        onClick = onContinueAfterReply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE91E63),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Continue")
                    }
                }
            } else if (sessionEnded && !isTyping) {
                Surface(color = Color.Transparent) {
                    Button(
                        onClick = onEndDay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE91E63),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Say Goodnight 🌙")
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = backgroundRes),
                contentDescription = "Scene background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .alpha(0.92f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = character?.name ?: "Mina",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    Button(
                        onClick = onToggleMusic,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF202845),
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (isMusicEnabled) "🎵 On" else "🔇 Off")
                    }
                }

                Text(
                    text = "A quiet night with ${character?.name ?: "her"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8C1EC)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF202845))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Affection: $affection ❤️", color = Color.White)
                            Text(relationshipLevel, color = statusColor)
                        }

                        Text("Mood: $mood", color = Color(0xFFB8C1EC))

                        HorizontalDivider(color = Color(0xFF3A4267))

                        Text(
                            text = "${character?.name ?: "Mina"} adapts to you: $personalityType",
                            color = Color(0xFFB8C1EC)
                        )

                        Text(
                            text = getMoodText(relationshipLevel),
                            color = Color(0xFFB8C1EC)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Image(
                    painter = painterResource(id = portraitRes),
                    contentDescription = character?.name ?: "Character portrait",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC1C2340)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = character?.name ?: "Mina",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFD6E7)
                    )

                    Text(
                        text = latestResponse.ifBlank { currentMessage },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            if (shouldShowMessageContainer) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3358))
                ) {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (visibleMessages.isNotEmpty()) {
                            items(visibleMessages) { message ->
                                ChatBubble(
                                    message = message,
                                    characterName = character?.name ?: "Mina"
                                )
                            }
                        } else if (visibleCurrentMessage.isNotEmpty()) {
                            item {
                                Text(
                                    text = character?.name ?: "Mina",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFFFD6E7)
                                )
                            }

                            item {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }

                        if (isTyping) {
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
                                ) {
                                    TypingIndicator(name = character?.name ?: "Mina")
                                }
                            }
                        }
                    }
                }
            }

            if (latestResponse.isNotBlank() && sessionEnded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A4267))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Her tone tonight",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFB8C1EC)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = latestResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            if (!isTyping && !sessionEnded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        activeScene.isNotBlank() -> {
                            sceneOptions.forEach { choice ->
                                Button(
                                    onClick = { onChooseSceneOption(choice) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(choice)
                                }
                            }
                        }

                        showDateQuestion -> {
                            dateOptions.forEach { date ->
                                Button(
                                    onClick = { onSelectFavoriteDate(date) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF7B5EA7),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(date)
                                }
                            }
                        }

                        showFoodQuestion -> {
                            foodOptions.forEach { food ->
                                Button(
                                    onClick = { onSelectFavoriteFood(food) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF8D6E63),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(food)
                                }
                            }
                        }

                        showTimeQuestion -> {
                            timeOptions.forEach { time ->
                                Button(
                                    onClick = { onSelectFavoriteTime(time) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF37474F),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(time)
                                }
                            }
                        }

                        else -> {
                            options.forEach { option ->
                                Button(
                                    onClick = { onChooseReply(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5561A8),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(option.text)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private fun getMoodText(relationshipLevel: String): String {
    return when (relationshipLevel) {
        "Stranger" -> "She is still getting used to your presence."
        "Friend" -> "She feels more comfortable around you now."
        "Close" -> "There is warmth and familiarity between you."
        else -> "She seems deeply attached to you tonight."
    }
}