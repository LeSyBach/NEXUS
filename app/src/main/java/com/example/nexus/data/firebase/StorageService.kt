package com.example.nexus.data.firebase

import android.net.Uri
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
     * Upload user avatar and return download URL.
     */
    suspend fun uploadAvatar(userId: String, imageUri: Uri): String {
        val ref = storage.reference
            .child("${Constants.STORAGE_AVATARS}/$userId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
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
