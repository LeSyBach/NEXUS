package com.example.nexus.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Archive
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
    data object Archive : Screen("archive", "Kho lưu trữ")
    data object Profile : Screen("profile", "Cá nhân")

    // ── Chat ──
    data object Conversation : Screen("conversation/{chatId}?action={action}", "Chat") {
        fun createRoute(chatId: String, action: String = "") =
            if (action.isEmpty()) "conversation/${Uri.encode(chatId)}"
            else "conversation/${Uri.encode(chatId)}?action=$action"
    }
    data object CreateGroup : Screen("create_group", "Tạo nhóm")
    data object LocketCamera : Screen("locket_camera", "Ảnh tức thì")
    data object GroupInfo : Screen("group_info/{groupId}", "Thông tin nhóm") {
        fun createRoute(groupId: String) = "group_info/$groupId"
    }
    data object ChatInfo : Screen("chat_info/{chatId}", "Thông tin chat") {
        fun createRoute(chatId: String) = "chat_info/${Uri.encode(chatId)}"
    }
    data object SharedMedia : Screen("shared_media/{chatId}?initialTab={initialTab}", "Nội dung chia sẻ") {
        fun createRoute(chatId: String, initialTab: String = "media") =
            "shared_media/${Uri.encode(chatId)}?initialTab=$initialTab"
    }

    // ── Contacts ──
    data object SearchUser : Screen("search_user", "Tìm kiếm")
    data object FriendRequests : Screen("friend_requests", "Lời mời kết bạn")
    data object OtherUserProfile : Screen("other_user_profile/{targetUserId}", "Hồ sơ") {
        fun createRoute(targetUserId: String) = "other_user_profile/$targetUserId"
    }

    // ── Profile ──
    data object EditProfile : Screen("edit_profile", "Chỉnh sửa")
    data object Settings : Screen("settings", "Cài đặt")
    data object ChangePassword : Screen("change_password", "Đổi mật khẩu")
    data object AddAccount : Screen("add_account", "Thêm tài khoản")

    // ── Call ──
    data object OngoingCall : Screen("ongoing_call/{callId}/{callType}?receiverId={receiverId}&receiverName={receiverName}", "Cuộc gọi") {
        fun createRoute(callId: String, callType: String = "voice", receiverId: String = "", receiverName: String = "") =
            "ongoing_call/$callId/$callType?receiverId=$receiverId&receiverName=$receiverName"
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
        screen = Screen.Archive,
        selectedIcon = Icons.Filled.Archive,
        unselectedIcon = Icons.Outlined.Archive,
        label = "Kho lưu trữ"
    ),
    BottomNavItem(
        screen = Screen.Profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        label = "Cá nhân"
    ),
)
