package com.example.nexus.data.firebase

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads media files to Cloudinary via unsigned upload using OkHttp.
 * Cloud: dt4h4ay7i, Upload Preset: nexus_chat
 */
@Singleton
class MediaUploader @Inject constructor() {

    companion object {
        private const val CLOUD_NAME = "dt4h4ay7i"
        private const val UPLOAD_PRESET = "nexus_chat"
        private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/upload"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Upload a file URI to Cloudinary and return the secure URL.
     *
     * @param context Android context for content resolver access
     * @param uri Local file URI (content:// or file://)
     * @param resourceType Cloudinary resource type: "image", "video", "raw", or "auto"
     * @return Cloudinary secure URL on success, null on failure
     */
    suspend fun upload(
        context: Context,
        uri: Uri,
        resourceType: String = "auto"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val tempFile = uriToFile(context, uri) ?: return@withContext null
            val fileName = getFileName(context, uri)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build()

            val url = if (resourceType != "auto") {
                "https://api.cloudinary.com/v1_1/$CLOUD_NAME/$resourceType/upload"
            } else {
                UPLOAD_URL
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            tempFile.delete()

            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                json.optString("secure_url", null)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Convert a content:// or file:// URI to a temporary File.
     */
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val tempFile = File.createTempFile("nexus_upload_", ".tmp", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract a display-friendly filename from the URI.
     */
    private fun getFileName(context: Context, uri: Uri): String {
        var name = "file"
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(idx) ?: "file"
                }
            }
        }
        if (name == "file") {
            name = uri.lastPathSegment ?: "file"
        }
        return name
    }

    /**
     * Detect MIME type from URI via ContentResolver.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }
}
