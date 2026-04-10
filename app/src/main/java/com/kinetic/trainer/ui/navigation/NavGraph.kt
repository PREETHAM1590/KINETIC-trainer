package com.kinetic.trainer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kinetic.trainer.ui.screens.ChatScreen
import com.kinetic.trainer.ui.screens.ClientDetailScreen
import com.kinetic.trainer.ui.screens.HomeScreen
import com.kinetic.trainer.ui.screens.WorkoutAssignmentScreen

@Composable
fun TrainerNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onClientClick = { clientId ->
                    navController.navigate(Screen.ClientDetail.createRoute(clientId))
                }
            )
        }

        composable(
            route = Screen.ClientDetail.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            ClientDetailScreen(
                clientId = clientId,
                onOpenChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) },
                onAssignWorkout = { id -> navController.navigate(Screen.WorkoutAssignment.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WorkoutAssignment.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            WorkoutAssignmentScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() },
                onAssigned = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: ""
            ChatScreen(
                clientId = clientId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
