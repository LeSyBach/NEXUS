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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.nexus.feature_chat.ui.ArchiveScreen
import com.example.nexus.feature_chat.ui.ChatListScreen
import com.example.nexus.feature_chat.ui.ConversationScreen
import com.example.nexus.feature_chat.ui.ChatInfoScreen
import com.example.nexus.feature_chat.ui.CreateGroupScreen
import com.example.nexus.feature_contact.ui.ContactListScreen
import com.example.nexus.feature_contact.ui.FriendRequestsScreen
import com.example.nexus.feature_contact.ui.OtherUserProfileScreen
import com.example.nexus.feature_contact.ui.SearchUserScreen
import com.example.nexus.feature_call.ui.OngoingCallScreen
import com.example.nexus.feature_call.ui.IncomingCallScreen
import com.example.nexus.feature_call.ui.rememberCallPermissions
import com.example.nexus.feature_call.viewmodel.CallViewModel
import com.example.nexus.feature_profile.ui.ProfileScreen
import com.example.nexus.feature_profile.ui.EditProfileScreen
import com.example.nexus.feature_profile.viewmodel.ProfileViewModel
import com.example.nexus.feature_admin.viewmodel.AdminViewModel
import com.example.nexus.feature_admin.ui.AccountLockedScreen
import com.example.nexus.feature_admin.ui.HelpSupportScreen
import com.example.nexus.feature_admin.ui.NotificationListScreen
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
                },
                onNavigateToLocked = {
                    navController.navigate(Screen.AccountLocked.route) {
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
                onNavigateToCamera = {
                    navController.navigate(Screen.LocketCamera.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationList.route)
                },
                onNavigateToTab = onNavigateToTab
            )
        }

        composable(Screen.LocketCamera.route) {
            val chatViewModel: com.example.nexus.feature_chat.viewmodel.ChatViewModel = hiltViewModel()
            com.example.nexus.feature_chat.ui.LocketCameraScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.OtherUserProfile.createRoute(userId))
                },
                onNavigateToTab = onNavigateToTab
            )
        }

        composable(Screen.Archive.route) {
            val chatViewModel: com.example.nexus.feature_chat.viewmodel.ChatViewModel = hiltViewModel()
            ArchiveScreen(
                viewModel = chatViewModel,
                onNavigateToConversation = { chatId ->
                    navController.navigate(Screen.Conversation.createRoute(chatId))
                },
                onNavigateToTab = onNavigateToTab
            )
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
                onNavigateToHelpSupport = {
                    navController.navigate(Screen.HelpSupport.route)
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
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("action") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val initialAction = backStackEntry.arguments?.getString("action") ?: ""
            val chatViewModel: com.example.nexus.feature_chat.viewmodel.ChatViewModel = hiltViewModel()
            ConversationScreen(
                chatId = chatId,
                viewModel = chatViewModel,
                initialAction = initialAction,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupInfo = { groupId ->
                    navController.navigate(Screen.ChatInfo.createRoute(groupId))
                },
                onStartCall = { receiverId, callType, receiverName ->
                    navController.navigate(Screen.OngoingCall.createRoute(
                        callId = java.util.UUID.randomUUID().toString(),
                        callType = callType,
                        receiverId = receiverId,
                        receiverName = receiverName
                    ))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.OtherUserProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.CreateGroup.route) {
            val groupViewModel: com.example.nexus.feature_chat.viewmodel.GroupViewModel = hiltViewModel()
            CreateGroupScreen(
                viewModel = groupViewModel,
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
            val groupViewModel: com.example.nexus.feature_chat.viewmodel.GroupViewModel = hiltViewModel()
            ChatInfoScreen(
                chatId = chatId,
                viewModel = chatViewModel,
                groupViewModel = groupViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { targetChatId ->
                    navController.navigate(Screen.Conversation.createRoute(targetChatId)) {
                        popUpTo(Screen.Conversation.createRoute(targetChatId)) { inclusive = true }
                    }
                },
                onNavigateToChatWithAction = { targetChatId, action ->
                    navController.navigate(Screen.Conversation.createRoute(targetChatId, action)) {
                        popUpTo(Screen.Conversation.createRoute(targetChatId)) { inclusive = true }
                    }
                },
                onNavigateToSharedMedia = { targetChatId, initialTab ->
                    navController.navigate(Screen.SharedMedia.createRoute(targetChatId, initialTab))
                },
                onStartCall = { receiverId, callType, receiverName ->
                    navController.navigate(Screen.OngoingCall.createRoute(
                        callId = java.util.UUID.randomUUID().toString(),
                        callType = callType,
                        receiverId = receiverId,
                        receiverName = receiverName
                    ))
                }
            )
        }

        composable(
            route = Screen.SharedMedia.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("initialTab") { type = NavType.StringType; defaultValue = "media" }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val initialTab = backStackEntry.arguments?.getString("initialTab") ?: "media"
            val sharedMediaViewModel: com.example.nexus.feature_chat.viewmodel.SharedMediaViewModel = hiltViewModel()
            com.example.nexus.feature_chat.ui.SharedMediaScreen(
                viewModel = sharedMediaViewModel,
                initialTab = initialTab,
                onNavigateBack = { navController.popBackStack() }
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
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.OtherUserProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.FriendRequests.route) {
            val contactViewModel: com.example.nexus.feature_contact.viewmodel.ContactViewModel = hiltViewModel()
            FriendRequestsScreen(
                viewModel = contactViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.OtherUserProfile.createRoute(userId))
                }
            )
        }

        composable(
            route = Screen.OtherUserProfile.route,
            arguments = listOf(navArgument("targetUserId") { type = NavType.StringType })
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: return@composable
            val otherUserViewModel: com.example.nexus.feature_contact.viewmodel.OtherUserProfileViewModel = hiltViewModel()
            OtherUserProfileScreen(
                viewModel = otherUserViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatId ->
                    navController.navigate(Screen.Conversation.createRoute(chatId))
                }
            )
        }

        // ══════ CALL ══════
        composable(
            route = Screen.OngoingCall.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType; defaultValue = "voice" },
                navArgument("receiverId") { type = NavType.StringType; defaultValue = "" },
                navArgument("receiverName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
            val callType = backStackEntry.arguments?.getString("callType") ?: "voice"
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverName = backStackEntry.arguments?.getString("receiverName") ?: ""

            // Share ViewModel from IncomingCall back stack if available (accepted from UI)
            // Otherwise create new (accepted from FCM notification or outgoing call)
            val incomingEntry = remember {
                try { navController.getBackStackEntry(Screen.IncomingCall.route) } catch (_: Exception) { null }
            }
            val callViewModel: CallViewModel = if (incomingEntry != null) {
                hiltViewModel(incomingEntry)
            } else {
                hiltViewModel()
            }

            // Permission check before initiating outgoing call
            var callStarted by remember { mutableStateOf(false) }
            val permissions = rememberCallPermissions(needCamera = callType == "video") {
                if (receiverId.isNotEmpty() && !callStarted) {
                    callStarted = true
                    callViewModel.startCall(receiverId, receiverName, callType)
                }
            }

            LaunchedEffect(Unit) {
                if (receiverId.isNotEmpty()) {
                    // Outgoing call — check permissions then start
                    if (permissions.allGranted && !callStarted) {
                        callStarted = true
                        callViewModel.startCall(receiverId, receiverName, callType)
                    } else if (!permissions.allGranted) {
                        permissions.requestPermissions()
                    }
                } else if (!callStarted && incomingEntry == null) {
                    // Incoming call accepted from FCM notification (IncomingCall not in back stack)
                    // Only call acceptCallFromNotification when NOT sharing ViewModel from IncomingCallScreen
                    callStarted = true
                    callViewModel.acceptCallFromNotification(callId, callType)
                }
            }

            // Auto-navigate back when call ends (skip initial IDLE state)
            val callState by callViewModel.callState.collectAsState()
            var wasCallActive by remember { mutableStateOf(false) }
            LaunchedEffect(callState) {
                if (callState == com.example.nexus.feature_call.viewmodel.CallState.OUTGOING ||
                    callState == com.example.nexus.feature_call.viewmodel.CallState.CONNECTED ||
                    callState == com.example.nexus.feature_call.viewmodel.CallState.INCOMING) {
                    wasCallActive = true
                }
                if (wasCallActive && callState == com.example.nexus.feature_call.viewmodel.CallState.ENDED) {
                    kotlinx.coroutines.delay(1500)
                    callViewModel.resetState()
                    // Pop both OngoingCall and IncomingCall from back stack
                    navController.popBackStack()
                    try { navController.popBackStack(Screen.IncomingCall.route, inclusive = true) } catch (_: Exception) {}
                }
            }

            OngoingCallScreen(
                callId = callId,
                callType = callType,
                viewModel = callViewModel,
                onNavigateBack = {
                    callViewModel.endCall()
                }
            )
        }

        composable(
            route = Screen.IncomingCall.route,
            arguments = listOf(navArgument("callId") { type = NavType.StringType })
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
            val callViewModel: CallViewModel = hiltViewModel()
            IncomingCallScreen(
                callId = callId,
                viewModel = callViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCallAccepted = { callType ->
                    // Don't pop IncomingCall — keep its ViewModel alive so OngoingCallScreen can share it
                    navController.navigate(Screen.OngoingCall.createRoute(callId, callType))
                }
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onNavigateToAddAccount = {
                    navController.navigate(Screen.AddAccount.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onSwitchAccount = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.AddAccount.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLocked = {
                    navController.navigate(Screen.AccountLocked.route) {
                        popUpTo(Screen.AddAccount.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ChangePassword.route) {
            val securityViewModel: com.example.nexus.feature_profile.viewmodel.SecurityViewModel = hiltViewModel()
            com.example.nexus.feature_profile.ui.ChangePasswordScreen(
                viewModel = securityViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ══════ ADMIN ══════
        composable(Screen.HelpSupport.route) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            HelpSupportScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationList.route) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            NotificationListScreen(
                viewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AccountLocked.route) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            AccountLockedScreen(
                viewModel = adminViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onSwitchAccount = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
