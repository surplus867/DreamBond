package com.example.dreambond.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dreambond.model.GirlfriendCharacter
import com.example.dreambond.ui.theme.DreamBondTheme

@Composable
fun CharacterSelectScreen(
    characters: List<GirlfriendCharacter>,
    onCharacterSelected: (GirlfriendCharacter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose Your Character",
            style = MaterialTheme.typography.headlineSmall
        )

        characters.forEach { character ->
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCharacterSelected(character) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = character.personality,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Tap to begin tonight's conversation",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CharacterSelectScreenPreview() {
    val sampleCharacters = listOf(
        GirlfriendCharacter(
            id = 1,
            name = "Mia",
            personality = "Warm and playful",
            introLine = "Hi, I have been waiting for you.",
        ),
        GirlfriendCharacter(
            id = 2,
            name = "Luna",
            personality = "Calm and thoughtful",
            introLine = "Good evening, how was your day?"
        )
    )

    DreamBondTheme {
        CharacterSelectScreen(
            characters = sampleCharacters,
            onCharacterSelected = {}
        )
    }
}