package com.example.dreambond.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dreambond.R
import com.example.dreambond.ChatBubble
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.GirlfriendCharacter
import com.example.dreambond.ui.theme.DreamBondTheme
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(
    character: GirlfriendCharacter?,
    affection: Int,
    relationshipLevel: String,
    currentMessage: String,
    latestResponse: String,
    sessionEnded: Boolean,
    isTyping: Boolean,
    messages: List<ChatMessage>,
    options: List<DialogueOption>,
    onChooseReply: (DialogueOption) -> Unit,
    onEndDay: () -> Unit,
    onSpeakLatestResponse: (String) -> Unit
) {
    LaunchedEffect(latestResponse, isTyping, sessionEnded) {
        if (sessionEnded && latestResponse.isNotBlank() && !isTyping) {
            delay(300)
            onSpeakLatestResponse(latestResponse)
        }
    }

    val statusColor = when (relationshipLevel) {
        "Stranger" -> Color.Gray
        "Friend" -> Color(0xFF4CAF50)
        "Close" -> Color(0xFF2196F3)
        else -> Color(0xFFE91E63)
    }

    val portraitRes = when (relationshipLevel) {
        "Stranger" -> R.drawable.mina_stranger
        "Friend" -> R.drawable.mina_friend
        "Close" -> R.drawable.mina_close
        else -> R.drawable.mina_special
    }

    val visibleMessages = messages.filter { it.text.isNotBlank() }
    val visibleCurrentMessage = currentMessage.trim()
    val shouldShowMessageContainer =
        !sessionEnded && (visibleMessages.isNotEmpty() || visibleCurrentMessage.isNotEmpty() || isTyping)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            if (sessionEnded && !isTyping) {
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
                        Text(
                            text = "End Day 🌙",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = character?.name ?: "Mina",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Text(
                text = "A quiet night with her",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB8C1EC)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF202845)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Affection: $affection ❤️",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )

                        Text(
                            text = relationshipLevel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor
                        )
                    }

                    HorizontalDivider(color = Color(0xFF3A4267))

                    Text(
                        text = getMoodText(relationshipLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8C1EC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Crossfade(targetState = portraitRes, label = "MinaImage") { target ->
                    Image(
                        painter = painterResource(id = target),
                        contentDescription = "Mina portrait",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (shouldShowMessageContainer) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 20.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A3358)
                    )
                ) {
                    val animatedText = if (visibleMessages.isEmpty() && visibleCurrentMessage.isNotEmpty()) {
                        typewriterText(visibleCurrentMessage)
                    } else {
                        ""
                    }

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
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            item {
                                Text(
                                    text = animatedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                            }
                        }

                        if (isTyping) {
                            item {
                                TypingIndicator(name = character?.name ?: "Mina")
                            }
                        }
                    }
                }
            }

            if (latestResponse.isNotBlank() && sessionEnded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 8.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3A4267)
                    )
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

            Spacer(modifier = Modifier.height(8.dp))

            if (!isTyping && !sessionEnded) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 132.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { option ->
                        Button(
                            onClick = { onChooseReply(option) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5561A8),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = option.text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (sessionEnded) 90.dp else 24.dp))
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


@Composable
fun typewriterText(
    text: String,
    typingSpeed: Long = 25L
): String {
    var displayText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        displayText = ""
        text.forEach { char ->
            displayText += char
            delay(typingSpeed)
        }
    }

    return displayText
}

@Preview(showBackground = true, showSystemUi = true, name = "Session Ended")
@Composable
private fun ChatScreenEndPreview() {
    val sampleCharacter = GirlfriendCharacter(
        id = 1,
        name = "Mia",
        personality = "Warm and playful",
        introLine = "Hi, I have been waiting for you."
    )

    DreamBondTheme {
        ChatScreen(
            character = sampleCharacter,
            affection = 10,
            relationshipLevel = "Close",
            currentMessage = "Today was really special.",
            latestResponse = "I had so much fun talking with you.",
            sessionEnded = true,
            isTyping = false,
            messages = listOf(
                ChatMessage(text = "Hi, I have been waiting for you.", isFromUser = false),
                ChatMessage(text = "I wanted to see you.", isFromUser = true)
            ),
            options = emptyList(),
            onChooseReply = {},
            onEndDay = {},
            onSpeakLatestResponse = {}
        )
    }
}