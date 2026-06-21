package com.example.nexus.feature_contact.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.core.utils.Resource
import com.example.nexus.feature_contact.viewmodel.ContactViewModel
import com.example.nexus.ui.theme.NexusPrimary
import com.example.nexus.ui.theme.nexusColors

// ══════════════════════════════════════════════════════════
//  FRIEND REQUESTS SCREEN – with tabs: ĐÃ NHẬN / ĐÃ GỬI
// ══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    viewModel: ContactViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {}
) {
    val nc = MaterialTheme.nexusColors
    val receivedState = viewModel?.friendRequests?.collectAsState()?.value ?: Resource.Idle
    val sentState = viewModel?.sentRequests?.collectAsState()?.value ?: Resource.Idle
    val snackbarHostState = remember { SnackbarHostState() }
    var processingId by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel?.respondResult?.collect { message ->
            processingId = null
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Lời mời kết bạn", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = nc.textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = nc.background)
                )
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = nc.background,
                    contentColor = NexusPrimary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = NexusPrimary
                            )
                        }
                    },
                    divider = {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(nc.divider))
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            val receivedCount = (receivedState as? Resource.Success)?.data?.size ?: 0
                            Text(
                                if (receivedCount > 0) "Đã nhận ($receivedCount)" else "Đã nhận",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = NexusPrimary,
                        unselectedContentColor = nc.textSecondary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            val sentCount = (sentState as? Resource.Success)?.data?.size ?: 0
                            Text(
                                if (sentCount > 0) "Đã gửi ($sentCount)" else "Đã gửi",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        selectedContentColor = NexusPrimary,
                        unselectedContentColor = nc.textSecondary
                    )
                }
            }
        },
        containerColor = nc.background
    ) { paddingValues ->
        when (selectedTab) {
            0 -> ReceivedRequestsTab(
                state = receivedState,
                processingId = processingId,
                onAccept = { requestId, fromUserId ->
                    processingId = requestId
                    viewModel?.respondToRequest(requestId, true, fromUserId)
                },
                onReject = { requestId, fromUserId ->
                    processingId = requestId
                    viewModel?.respondToRequest(requestId, false, fromUserId)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            1 -> SentRequestsTab(
                state = sentState,
                onCancel = { targetUserId ->
                    viewModel?.cancelFriendRequest(targetUserId)
                },
                onNavigateToProfile = onNavigateToProfile,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}
