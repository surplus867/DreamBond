package com.example.dreambond.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dreambond.ui.theme.DreamBondTheme

@Composable
fun EndDayScreen(
    day: Int,
    affection: Int,
    onNextDay: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { 
        Text(
            text = "Day: $day",
            modifier = Modifier.padding(top = 12.dp)
        )
        
        Text(
            text = "Affection: $affection",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        
        Button(onClick = onNextDay) { 
            Text("Next Day")
        }

        Button(
            onClick = onBackToHome,
            modifier = Modifier.padding(top = 12.dp)
        ) { 
            Text("Back to Home")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun EndDayScreenPreview() {
    DreamBondTheme {
        EndDayScreen(
            day = 3,
            affection = 12,
            onNextDay = {},
            onBackToHome = {}
        )
    }
}