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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
    options: List<DialogueOption>,
    onChooseReply: (DialogueOption) -> Unit,
    onEndDay: () -> Unit
) {
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 20.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A3358)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = character?.name ?: "Mina",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFD6E7)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val animatedText = typewriterText(currentMessage)

                    Text(
                        text = animatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
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

            if (!sessionEnded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mina",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFD6E7)
                    )
                    TypingDots()
                }

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
                        Text(
                            text = option.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onEndDay,
                    modifier = Modifier.fillMaxWidth(),
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
fun TypingDots() {
    var dots by remember { mutableStateOf(".") }

    LaunchedEffect(Unit) {
        while (true) {
            dots = "."
            delay(300)
            dots = ".."
            delay(300)
            dots = "..."
            delay(300)
        }
    }

    Text(
        text = dots,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White
    )
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
            options = emptyList(),
            onChooseReply = {},
            onEndDay = {}
        )
    }
}