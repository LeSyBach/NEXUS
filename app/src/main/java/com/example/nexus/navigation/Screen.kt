package com.example.nexus.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines all navigation destinations in the NEXUS app.
 */
sealed class Screen(
    val route: String,
    val title: String = "",
) {
    // ── Auth ──
    data object Login : Screen("login", "Đăng nhập")
    data object Register : Screen("register", "Đăng ký")

    // ── Main tabs ──
    data object ChatList : Screen("chat_list", "Tin nhắn")
    data object Contacts : Screen("contacts", "Danh bạ")
    data object Groups : Screen("groups", "Nhóm")
    data object Profile : Screen("profile", "Cá nhân")

    // ── Chat ──
    data object Conversation : Screen("conversation/{chatId}", "Chat") {
        fun createRoute(chatId: String) = "conversation/$chatId"
    }
    data object CreateGroup : Screen("create_group", "Tạo nhóm")
    data object GroupInfo : Screen("group_info/{groupId}", "Thông tin nhóm") {
        fun createRoute(groupId: String) = "group_info/$groupId"
    }

    // ── Contacts ──
    data object SearchUser : Screen("search_user", "Tìm kiếm")
    data object FriendRequests : Screen("friend_requests", "Lời mời kết bạn")

    // ── Profile ──
    data object EditProfile : Screen("edit_profile", "Chỉnh sửa")

    // ── Call ──
    data object OngoingCall : Screen("ongoing_call/{callId}", "Cuộc gọi") {
        fun createRoute(callId: String) = "ongoing_call/$callId"
    }
    data object IncomingCall : Screen("incoming_call/{callId}", "Cuộc gọi đến") {
        fun createRoute(callId: String) = "incoming_call/$callId"
    }
}

/**
 * Bottom navigation items for the main screen.
 */
data class BottomNavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val badgeCount: Int = 0,
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.ChatList,
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat,
        label = "Tin nhắn"
    ),
    BottomNavItem(
        screen = Screen.Contacts,
        selectedIcon = Icons.Filled.Contacts,
        unselectedIcon = Icons.Outlined.Contacts,
        label = "Danh bạ"
    ),
    BottomNavItem(
        screen = Screen.Groups,
        selectedIcon = Icons.Filled.Group,
        unselectedIcon = Icons.Outlined.Group,
        label = "Nhóm"
    ),
    BottomNavItem(
        screen = Screen.Profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        label = "Cá nhân"
    ),
)
