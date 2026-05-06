package com.example.nexus.data.repository

import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.model.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Authentication and User management.
 * Acts as the single source of truth for auth operations.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val firestoreService: FirestoreService
) {
    val currentUser: FirebaseUser?
        get() = authService.currentUser

    val currentUserId: String?
        get() = authService.currentUserId

    fun observeAuthState(): Flow<FirebaseUser?> = authService.observeAuthState()

    suspend fun login(email: String, password: String) {
        authService.signInWithEmail(email, password)
    }

    suspend fun register(email: String, password: String, username: String) {
        // 1. Create user in Firebase Auth
        val firebaseUser = authService.registerWithEmail(email, password)

        // 2. Update display name in Firebase Auth Profile
        authService.updateDisplayName(username)

        // 3. Save user data to Firestore Database
        val user = User(
            uid = firebaseUser.uid,
            email = email,
            username = username.lowercase().replace(" ", "_"), // Simple username generation
            displayName = username,
            status = "online",
            avatarUrl = "" // Empty avatar for now
        )
        firestoreService.createUser(user)
    }

    fun logout() {
        authService.signOut()
    }
}
