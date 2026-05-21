package com.kinetic.trainer.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kinetic.trainer.ui.screens.ChatScreen
import com.kinetic.trainer.ui.screens.ClientDetailScreen
import com.kinetic.trainer.ui.screens.HomeScreen
import com.kinetic.trainer.ui.screens.LoginScreen
import com.kinetic.trainer.ui.screens.PrivacySettingsScreen
import com.kinetic.trainer.ui.screens.WorkoutAssignmentScreen

@Composable
fun TrainerNavGraph(navController: NavHostController, intent: Intent? = null) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val startRoute = if (auth.currentUser != null) Screen.Home.route else Screen.Login.route

    LaunchedEffect(intent) {
        val clientId = intent?.getStringExtra("clientId")
        if (clientId != null && auth.currentUser != null) {
            navController.navigate(Screen.Chat.createRoute(clientId)) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onClientClick = { clientId ->
                    navController.navigate(Screen.ClientDetail.createRoute(clientId))
                },
                onPrivacyClick = {
                    navController.navigate(Screen.PrivacySettings.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.ClientDetail.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            ClientDetailScreen(
                clientId = clientId,
                onOpenChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) },
                onAssignWorkout = { id -> navController.navigate(Screen.WorkoutAssignment.createRoute(id)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.WorkoutAssignment.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            WorkoutAssignmentScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() },
                onAssigned = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            ChatScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
