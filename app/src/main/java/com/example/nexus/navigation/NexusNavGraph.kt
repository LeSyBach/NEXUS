package com.example.nexus.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.nexus.feature_auth.ui.LoginScreen
import com.example.nexus.feature_auth.ui.RegisterScreen
import com.example.nexus.feature_auth.viewmodel.AuthViewModel
import com.example.nexus.feature_chat.ui.ChatListScreen
import com.example.nexus.feature_chat.ui.ConversationScreen
import com.example.nexus.feature_chat.ui.ChatInfoScreen
import com.example.nexus.feature_chat.ui.CreateGroupScreen
import com.example.nexus.feature_contact.ui.ContactListScreen
import com.example.nexus.feature_contact.ui.FriendRequestsScreen
import com.example.nexus.feature_contact.ui.SearchUserScreen
import com.example.nexus.feature_call.ui.CallHistoryScreen
import com.example.nexus.feature_profile.ui.ProfileScreen
import com.example.nexus.feature_profile.ui.EditProfileScreen
import com.example.nexus.feature_profile.viewmodel.ProfileViewModel
import com.example.nexus.ui.theme.nexusColors
/**
 * Main navigation graph for NEXUS.
 * Handles authentication flow and main app navigation.
 */
@Composable
fun NexusNavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
) {
    val startDestination = if (isLoggedIn) Screen.ChatList.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
    ) {
        // ══════ AUTH ══════
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ══════ MAIN TABS ══════
        val onNavigateToTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                // Pop up to the start destination of the graph to
                // avoid building up a large stack of destinations
                // on the back stack as users select items
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination when
                // reselecting the same item
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }
        }

        composable(Screen.ChatList.route) {
            val chatViewModel: com.example.nexus.feature_chat.viewmodel.ChatViewModel = hiltViewModel()
            ChatListScreen(
                viewModel = chatViewModel,
                onNavigateToConversation = { chatId ->
                    navController.navigate(Screen.Conversation.createRoute(chatId))
                },
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.SearchUser.route)
                },
                onNavigateToTab = onNavigateToTab
            )
        }

        composable(Screen.Contacts.route) {
            val contactViewModel: com.example.nexus.feature_contact.viewmodel.ContactViewModel = hiltViewModel()
            ContactListScreen(
                viewModel = contactViewModel,
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Conversation.createRoute(chatId))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.SearchUser.route)
                },
                onNavigateToFriendRequests = {
                    navController.navigate(Screen.FriendRequests.route)
                },
                onNavigateToTab = onNavigateToTab
            )
        }

        composable(Screen.Groups.route) {
            val nc = MaterialTheme.nexusColors
            androidx.compose.material3.Scaffold(
                bottomBar = {
                    com.example.nexus.ui.components.NexusBottomBar(
                        currentRoute = Screen.Groups.route,
                        onNavigate = onNavigateToTab
                    )
                },
                containerColor = nc.background
            ) { paddingValues ->
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text("Màn hình Nhóm đang phát triển", color = nc.textPrimary)
                }
            }
        }

        composable(Screen.Profile.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateToEdit = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToTab = onNavigateToTab
            )
        }

        // ══════ CHAT ══════
        composable(
            route = Screen.Conversation.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val chatViewModel: com.example.nexus.feature_chat.viewmodel.ChatViewModel = hiltViewModel()
            ConversationScreen(
                chatId = chatId,
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupInfo = { groupId ->
                    navController.navigate(Screen.ChatInfo.createRoute(groupId))
                }
            )
        }

        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { chatId ->
                    navController.navigate(Screen.Conversation.createRoute(chatId)) {
                        popUpTo(Screen.ChatList.route)
                    }
                }
            )
        }

        // ══════ CONTACTS ══════
        composable(
            route = Screen.ChatInfo.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val chatViewModel: com.example.nexus.feature_chat.viewmodel.ChatViewModel = hiltViewModel()
            ChatInfoScreen(
                chatId = chatId,
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { navController.popBackStack() }
            )
        }

        composable(Screen.SearchUser.route) {
            val contactViewModel: com.example.nexus.feature_contact.viewmodel.ContactViewModel = hiltViewModel()
            SearchUserScreen(
                viewModel = contactViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Conversation.createRoute(chatId)) {
                        popUpTo(Screen.Contacts.route)
                    }
                }
            )
        }

        composable(Screen.FriendRequests.route) {
            val contactViewModel: com.example.nexus.feature_contact.viewmodel.ContactViewModel = hiltViewModel()
            FriendRequestsScreen(
                viewModel = contactViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ══════ PROFILE ══════
        composable(Screen.EditProfile.route) {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            EditProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: com.example.nexus.feature_profile.viewmodel.SettingsViewModel = hiltViewModel()
            com.example.nexus.feature_profile.ui.SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
