package com.example.dreambond

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dreambond.ui.ChatMessage

@Composable
fun ChatBubble(
    message: ChatMessage,
    characterName: String
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isFromUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isFromUser) {
                        Color(0xAA5561A8)
                    } else {
                        Color(0xAA2A3358)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (message.isFromUser) "You" else characterName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.isFromUser) Color.White else Color(0xFFFFD6E7)
                    )

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}
