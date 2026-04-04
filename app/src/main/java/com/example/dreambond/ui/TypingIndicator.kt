package com.example.dreambond.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TypingIndicator(name: String = "Mina") {
    var dots by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            dots = ""
            delay(300)
            dots = "."
            delay(300)
            dots = ".."
            delay(300)
            dots = "..."
            delay(300)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$name is typing$dots",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFFD6E7)
        )
    }
}