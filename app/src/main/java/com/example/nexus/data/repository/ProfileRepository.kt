package com.example.nexus.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.nexus.core.utils.Constants
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.firebase.MediaUploader
import com.example.nexus.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreService: FirestoreService,
    private val authService: AuthService,
    private val mediaUploader: MediaUploader,
    private val authRepository: AuthRepository
) {
    val currentUserId: String? get() = authService.currentUserId

    fun observeCurrentUser() = authService.currentUserId?.let {
        firestoreService.observeUser(it)
    }

    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return firestoreService.getUser(uid)
    }

    suspend fun uploadAvatar(imageUri: Uri): String {
        Log.d("AvatarUpload", "Uploading to Cloudinary, URI: $imageUri")
        val url = mediaUploader.upload(context, imageUri, "image")
            ?: throw IllegalStateException("Tải ảnh lên Cloudinary thất bại")
        Log.d("AvatarUpload", "Cloudinary URL: $url")
        return url
    }

    suspend fun updateProfile(displayName: String, phone: String, bio: String, avatarUrl: String? = null) {
        val uid = currentUserId ?: return
        val updates = buildMap<String, Any> {
            if (displayName.isNotBlank()) put("displayName", displayName)
            if (phone.isNotBlank()) put("phone", phone)
            put("bio", bio)
            if (avatarUrl != null) put("avatarUrl", avatarUrl)
        }
        firestoreService.updateUser(uid, updates)
    }

    suspend fun requestAccountDeletion() {
        val uid = currentUserId ?: return
        firestoreService.updateUser(uid, mapOf(
            "status" to Constants.USER_STATUS_PENDING_DELETION,
            "deletedAt" to Timestamp.now()
        ))
        authRepository.logout()
    }

    suspend fun logout() {
        authRepository.logout()
    }
}
