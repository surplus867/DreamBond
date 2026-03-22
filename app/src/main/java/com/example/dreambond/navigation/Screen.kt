package com.example.dreambond.navigation


sealed class Screen(val route: String) {
    data object Name : Screen("home")
    data object CharacterSelect :
        Screen("character_select")

    data object Chat : Screen("chat")
    data object EndDay : Screen("end_day")
}