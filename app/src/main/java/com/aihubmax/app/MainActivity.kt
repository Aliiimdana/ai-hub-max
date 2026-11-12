package com.aihubmax.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aihubmax.core.design.theme.AiHubMaxTheme
import com.aihubmax.features.agents.AgentBuilderRoute
import com.aihubmax.features.api_manager.ApiManagerRoute
import com.aihubmax.features.chat.ChatRoute
import com.aihubmax.features.documents.DocumentAiRoute
import com.aihubmax.features.free_ai.FreeAiRadarRoute
import com.aihubmax.features.home.HomeDashboardRoute
import com.aihubmax.features.image_studio.ImageStudioRoute
import com.aihubmax.features.usage.UsageCostRoute
import com.aihubmax.features.video_studio.VideoAutopilotRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isPersian by rememberSaveable { mutableStateOf(true) }
            val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
            val navController = rememberNavController()

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AiHubMaxTheme {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeDashboardRoute(
                                isPersian = isPersian,
                                onToggleLanguage = { isPersian = !isPersian },
                                onOpenApiCenter = { navController.navigate("api_manager") },
                                onOpenFreeAiRadar = { navController.navigate("free_ai") },
                                onOpenChat = { prompt ->
                                    val encoded = Uri.encode(prompt)
                                    navController.navigate("chat?prompt=$encoded")
                                },
                                onOpenImageStudio = { navController.navigate("image_studio") },
                                onOpenVideoStudio = { navController.navigate("video_studio") },
                                onOpenAgents = { navController.navigate("agents") },
                                onOpenDocuments = { navController.navigate("documents") },
                                onOpenUsage = { navController.navigate("usage") }
                            )
                        }
                        composable("api_manager") {
                            ApiManagerRoute(
                                isPersian = isPersian,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("free_ai") {
                            FreeAiRadarRoute(
                                isPersian = isPersian,
                                onBack = { navController.popBackStack() },
                                onConnectProvider = { navController.navigate("api_manager") }
                            )
                        }
                        composable(
                            route = "chat?prompt={prompt}",
                            arguments = listOf(navArgument("prompt") {
                                type = NavType.StringType
                                defaultValue = ""
                            })
                        ) { backStackEntry ->
                            val initialPrompt = backStackEntry.arguments?.getString("prompt").orEmpty()
                            ChatRoute(
                                isPersian = isPersian,
                                initialPrompt = initialPrompt,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("image_studio") {
                            ImageStudioRoute(
                                isPersian = isPersian,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("video_studio") {
                            VideoAutopilotRoute(
                                isPersian = isPersian,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("agents") {
                            AgentBuilderRoute(
                                isPersian = isPersian,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("documents") {
                            DocumentAiRoute(
                                isPersian = isPersian,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("usage") {
                            UsageCostRoute(
                                isPersian = isPersian,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
