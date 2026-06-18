package com.example.nexus.feature_admin.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nexus.feature_admin.viewmodel.AdminViewModel
import com.example.nexus.ui.theme.nexusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val nc = MaterialTheme.nexusColors
    val feedbackState by viewModel.feedbackState.collectAsState()
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trợ giúp & Hỗ trợ", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = nc.background,
                    titleContentColor = nc.textPrimary,
                    navigationIconContentColor = nc.textPrimary
                )
            )
        },
        containerColor = nc.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Gửi yêu cầu hỗ trợ",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = nc.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mô tả vấn đề bạn đang gặp phải, chúng tôi sẽ phản hồi trong thời gian sớm nhất.",
                fontSize = 14.sp,
                color = nc.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("Tiêu đề", color = nc.textTertiary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C6FF),
                    unfocusedBorderColor = nc.divider,
                    focusedTextColor = nc.textPrimary,
                    unfocusedTextColor = nc.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Mô tả chi tiết vấn đề...", color = nc.textTertiary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C6FF),
                    unfocusedBorderColor = nc.divider,
                    focusedTextColor = nc.textPrimary,
                    unfocusedTextColor = nc.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        viewModel.submitFeedback("support", subject, content)
                        showSuccess = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C6FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Gửi yêu cầu", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showSuccess && feedbackState?.isSuccess == true) {
        AlertDialog(
            onDismissRequest = {
                showSuccess = false
                viewModel.resetFeedbackState()
                subject = ""
                content = ""
                onNavigateBack()
            },
            containerColor = nc.cardBg,
            title = { Text("Gửi thành công", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Yêu cầu hỗ trợ đã được gửi. Chúng tôi sẽ phản hồi trong thời gian sớm nhất.", color = nc.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showSuccess = false
                    viewModel.resetFeedbackState()
                    subject = ""
                    content = ""
                    onNavigateBack()
                }) {
                    Text("OK", color = Color(0xFF00C6FF))
                }
            }
        )
    }

    if (showSuccess && feedbackState?.isFailure == true) {
        AlertDialog(
            onDismissRequest = { showSuccess = false; viewModel.resetFeedbackState() },
            containerColor = nc.cardBg,
            title = { Text("Gửi thất bại", color = nc.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Có lỗi xảy ra, vui lòng thử lại sau.", color = nc.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showSuccess = false; viewModel.resetFeedbackState() }) {
                    Text("OK", color = Color(0xFF00C6FF))
                }
            }
        )
    }
}
