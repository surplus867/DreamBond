package com.example.dreambond.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.dreambond.GameViewModel
import com.example.dreambond.GameViewModelFactory
import com.example.dreambond.data.GameRepository
import com.example.dreambond.data.local.DreamBondDatabase
import com.example.dreambond.navigation.Screen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    database: DreamBondDatabase
) {
    val repository = GameRepository(database.gameProgressDao())
    val factory = GameViewModelFactory(repository)
    val gameViewModel: GameViewModel = viewModel(factory = factory)
    val uiState by gameViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartClick = {
                    navController.navigate(Screen.CharacterSelect.route)
                }
            )
        }

        composable(Screen.CharacterSelect.route) {
            CharacterSelectScreen(
                characters = gameViewModel.characters,
                onCharacterSelected = { character ->
                    gameViewModel.selectCharacter(character)
                    navController.navigate(Screen.Chat.route)
                }
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                character = uiState.selectedCharacter,
                affection = uiState.affection,
                relationshipLevel = gameViewModel.getRelationShipLevel(),
                currentMessage = uiState.currentMessage,
                latestResponse = uiState.latestResponse,
                sessionEnded = uiState.sessionEnded,
                options = gameViewModel.dialogueOptions,
                onChooseReply = { option ->
                    gameViewModel.chooseReply(option)
                },
                onEndDay = {
                    gameViewModel.saveProgress()
                    navController.navigate(Screen.EndDay.route)
                }
            )
        }

        composable(Screen.EndDay.route) {
            EndDayScreen(
                day = uiState.day,
                affection = uiState.affection,
                onNextDay = {
                    gameViewModel.saveProgress()
                    gameViewModel.nextDay()
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.EndDay.route) { inclusive = true }

                    }
                },
                onBackToHome = {
                    gameViewModel.resetGame()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}