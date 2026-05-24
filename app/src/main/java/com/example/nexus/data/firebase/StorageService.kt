package com.example.nexus.data.firebase

import android.net.Uri
import android.util.Log
import com.example.nexus.core.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firebase Storage operations for file uploads.
 */
@Singleton
class StorageService @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Upload user avatar from ByteArray and return download URL.
     * Uses putBytes() — most reliable across all devices.
     * Gets download URL from UploadTask metadata to avoid "Object does not exist" race condition.
     */
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String {
        val path = "${Constants.STORAGE_AVATARS}/$userId.jpg"
        val ref = storage.reference.child(path)
        Log.d("AvatarUpload", "Storage path: $path, bytes: ${imageBytes.size}")

        val taskSnapshot = ref.putBytes(imageBytes).await()
        Log.d("AvatarUpload", "putBytes done, transferred: ${taskSnapshot.bytesTransferred}")

        // Verify file exists by reading metadata before fetching URL
        val metadata = ref.metadata.await()
        Log.d("AvatarUpload", "Metadata size: ${metadata.sizeBytes}")

        val url = ref.downloadUrl.await().toString()
        Log.d("AvatarUpload", "Download URL: $url")
        return url
    }

    /**
     * Upload chat image and return download URL.
     */
    suspend fun uploadChatImage(chatId: String, fileName: String, imageUri: Uri): String {
        val ref = storage.reference
            .child("${Constants.STORAGE_CHAT_IMAGES}/$chatId/$fileName")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload chat file and return download URL.
     */
    suspend fun uploadChatFile(chatId: String, fileName: String, fileUri: Uri): String {
        val ref = storage.reference
            .child("${Constants.STORAGE_CHAT_FILES}/$chatId/$fileName")
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload voice message and return download URL.
     */
    suspend fun uploadVoiceMessage(chatId: String, fileName: String, fileUri: Uri): String {
        val ref = storage.reference
            .child("${Constants.STORAGE_VOICE_MESSAGES}/$chatId/$fileName")
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Upload group avatar and return download URL.
     */
    suspend fun uploadGroupAvatar(groupId: String, imageUri: Uri): String {
        val ref = storage.reference
            .child("${Constants.STORAGE_AVATARS}/groups/$groupId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Delete a file from storage.
     */
    suspend fun deleteFile(path: String) {
        storage.reference.child(path).delete().await()
    }
}
