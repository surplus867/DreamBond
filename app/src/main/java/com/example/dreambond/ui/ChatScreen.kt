package com.example.dreambond.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.GirlfriendCharacter
import com.example.dreambond.ui.theme.DreamBondTheme

@Composable
fun ChatScreen(
    character: GirlfriendCharacter?,
    affection: Int,
    currentMessage: String,
    latestResponse: String,
    sessionEnded: Boolean,
    options: List<DialogueOption>,
    onChooseReply: (DialogueOption) -> Unit,
    onEndDay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = character?.name ?: "Unknown",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Affection: $affection",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentMessage,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )

            if (latestResponse.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = latestResponse,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!sessionEnded) {
                options.forEach { option ->
                    Button(
                        onClick = { onChooseReply(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option.text)
                    }
                }
            } else {
                Button(
                    onClick = onEndDay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("End Day")
                }
            }
        }
    }
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
            currentMessage = "Today was really special.",
            latestResponse = "I had so much fun talking with you.",
            sessionEnded = true,
            options = emptyList(),
            onChooseReply = {},
            onEndDay = {}
        )
    }
}