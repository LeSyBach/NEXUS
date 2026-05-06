package com.example.nexus.data.repository

import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val authService: AuthService
) {
    val currentUserId: String? get() = authService.currentUserId

    fun observeCurrentUser() = authService.currentUserId?.let {
        firestoreService.observeUser(it)
    }

    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return firestoreService.getUser(uid)
    }

    suspend fun updateProfile(displayName: String, phone: String, bio: String) {
        val uid = currentUserId ?: return
        val updates = buildMap<String, Any> {
            if (displayName.isNotBlank()) put("displayName", displayName)
            if (phone.isNotBlank()) put("phone", phone)
            put("bio", bio)
        }
        firestoreService.updateUser(uid, updates)
    }

    fun logout() {
        authService.signOut()
    }
}
