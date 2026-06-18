package com.example.nexus.feature_chat.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.nexus.ui.theme.NexusColors
import com.example.nexus.ui.theme.nexusColors
import com.example.nexus.feature_chat.viewmodel.ChatViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocketCameraScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val nc = MaterialTheme.nexusColors

    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        CameraContent(viewModel = viewModel, onNavigateBack = onNavigateBack, context = context, nc = nc)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Cần quyền Camera để tiếp tục", color = Color.White)
        }
    }
}

@Composable
private fun CameraContent(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    context: Context,
    nc: NexusColors
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var flashEnabled by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }

    if (capturedUri != null) {
        PreviewScreen(
            uri = capturedUri!!,
            onCancel = { capturedUri = null },
            onPost = { uri, caption ->
                viewModel.uploadAndPostStory(context, uri, caption)
                onNavigateBack()
            }
        )
    } else {
        Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text("Ảnh tức thì mới", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp)) // To keep title centered if needed
        }

        // Camera Preview (Rounded like Locket)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .fillMaxHeight(0.6f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(48.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageCapture
                            )
                            imageCapture.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                        } catch (e: Exception) {
                            Log.e("CameraX", "Binding failed", e)
                        }
                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    // Đọc state ở ngay đầu block để Compose track được sự thay đổi
                    val currentLens = lensFacing
                    val currentFlash = flashEnabled
                    
                    val executor = ContextCompat.getMainExecutor(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.Builder().requireLensFacing(currentLens).build()
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageCapture
                            )
                            imageCapture.flashMode = if (currentFlash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                        } catch (e: Exception) {
                            Log.e("CameraX", "Update binding failed", e)
                        }
                    }, executor)
                }
            )
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    flashEnabled = !flashEnabled
                    imageCapture.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                },
                modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.5f), CircleShape).size(48.dp)
            ) {
                Icon(
                    if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }

            // Capture Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, Color.LightGray, CircleShape)
                    .padding(8.dp)
                    .background(Color.White, CircleShape)
                    .clickable {
                        val outputDirectory = context.cacheDir
                        val photoFile = File(
                            outputDirectory,
                            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
                        )
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capturedUri = output.savedUri
                                }
                                override fun onError(e: ImageCaptureException) {
                                    Log.e("CameraX", "Capture failed", e)
                                }
                            }
                        )
                    }
            )

            IconButton(
                onClick = {
                    lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                },
                modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.5f), CircleShape).size(48.dp)
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White)
            }
        }

        // Removed audience selector chip
    }
    }
}

@Composable
fun PreviewScreen(
    uri: Uri,
    onCancel: () -> Unit,
    onPost: (Uri, String?) -> Unit
) {
    var captionText by remember { mutableStateOf("") }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Image
        coil.compose.AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .fillMaxHeight(0.6f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(48.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        
        // Text field near bottom of image
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = captionText,
                onValueChange = { if (it.length <= 60) captionText = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (captionText.isEmpty()) {
                        Text("Thêm văn bản...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    } else {
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${captionText.length}/60", color = Color.White)
            Button(
                onClick = { onPost(uri, captionText.takeIf { it.isNotBlank() }) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C6FF))
            ) {
                Text("Đăng", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
