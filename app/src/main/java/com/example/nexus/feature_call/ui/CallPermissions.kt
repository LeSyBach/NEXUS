package com.example.nexus.feature_call.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Call permissions state holder.
 */
data class CallPermissionsState(
    val allGranted: Boolean = false,
    val audioGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val requestPermissions: () -> Unit = {}
)

/**
 * Remember and manage call permissions (RECORD_AUDIO + CAMERA).
 * For voice calls, only RECORD_AUDIO is required.
 * For video calls, both RECORD_AUDIO and CAMERA are required.
 */
@Composable
fun rememberCallPermissions(
    needCamera: Boolean = false,
    onAllGranted: () -> Unit = {}
): CallPermissionsState {
    val context = LocalContext.current

    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (needCamera) add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }.toTypedArray()

    var audioGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.RECORD_AUDIO)) }
    var cameraGranted by remember { mutableStateOf(!needCamera || hasPermission(context, Manifest.permission.CAMERA)) }
    var requested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        audioGranted = results[Manifest.permission.RECORD_AUDIO] == true
        cameraGranted = !needCamera || results[Manifest.permission.CAMERA] == true
        requested = true

        if (audioGranted && cameraGranted) {
            onAllGranted()
        }
    }

    val allGranted = audioGranted && cameraGranted

    // Auto-check on first composition if already granted
    LaunchedEffect(Unit) {
        audioGranted = hasPermission(context, Manifest.permission.RECORD_AUDIO)
        cameraGranted = !needCamera || hasPermission(context, Manifest.permission.CAMERA)
        if (audioGranted && cameraGranted) {
            onAllGranted()
        }
    }

    return CallPermissionsState(
        allGranted = allGranted,
        audioGranted = audioGranted,
        cameraGranted = cameraGranted,
        shouldShowRationale = requested && !allGranted,
        requestPermissions = { launcher.launch(permissions) }
    )
}

private fun hasPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
