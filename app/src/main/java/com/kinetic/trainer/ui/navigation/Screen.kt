package com.kinetic.trainer.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object ClientDetail : Screen("client_detail/{clientId}") {
        fun createRoute(clientId: String) = "client_detail/$clientId"
    }
    object WorkoutAssignment : Screen("workout_assignment/{clientId}") {
        fun createRoute(clientId: String) = "workout_assignment/$clientId"
    }
    object Chat : Screen("chat/{clientId}") {
        fun createRoute(clientId: String) = "chat/$clientId"
    }
    object PrivacySettings : Screen("privacy_settings")
}
