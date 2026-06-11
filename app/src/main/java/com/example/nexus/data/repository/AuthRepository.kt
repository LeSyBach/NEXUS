package com.example.nexus.data.repository

import com.example.nexus.core.utils.AccountManager
import com.example.nexus.core.utils.Constants
import com.example.nexus.core.utils.SavedAccount
import com.example.nexus.data.firebase.AuthService
import com.example.nexus.data.firebase.FirestoreService
import com.example.nexus.data.model.User
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Authentication and User management.
 * Acts as the single source of truth for auth operations.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val firestoreService: FirestoreService,
    private val accountManager: AccountManager
) {
    val currentUser: FirebaseUser?
        get() = authService.currentUser

    val currentUserId: String?
        get() = authService.currentUserId

    fun observeAuthState(): Flow<FirebaseUser?> = authService.observeAuthState()

    suspend fun login(email: String, password: String) {
        authService.signInWithEmail(email, password)

        val userId = authService.currentUserId
        if (userId != null) {
            // Check if account is pending deletion → auto reactivate
            val user = firestoreService.getUser(userId)
            if (user?.status == Constants.USER_STATUS_PENDING_DELETION) {
                firestoreService.updateUser(userId, mapOf(
                    "status" to Constants.USER_STATUS_ACTIVE,
                    "deletedAt" to FieldValue.delete()
                ))
                Log.d("AuthRepository", "Account reactivated for user $userId")
            }

            // Save credential for account switching
            accountManager.addAccount(
                SavedAccount(
                    email = email,
                    encryptedPassword = password,
                    displayName = user?.displayName ?: "",
                    avatarUrl = user?.avatarUrl ?: ""
                )
            )
        }

        saveFcmToken()
    }

    suspend fun register(email: String, password: String, username: String) {
        val firebaseUser = authService.registerWithEmail(email, password)
        authService.updateDisplayName(username)
        val user = User(
            uid = firebaseUser.uid,
            email = email,
            username = username.lowercase().replace(" ", "_"),
            displayName = username,
            status = Constants.USER_STATUS_ACTIVE,
            avatarUrl = ""
        )
        firestoreService.createUser(user)

        accountManager.addAccount(
            SavedAccount(
                email = email,
                encryptedPassword = password,
                displayName = username,
                avatarUrl = ""
            )
        )

        saveFcmToken()
    }

    suspend fun logout() {
        try {
            val userId = authService.currentUserId
            if (userId != null) {
                firestoreService.updateUser(userId, mapOf(
                    "fcmToken" to "",
                    "status" to Constants.USER_STATUS_OFFLINE,
                    "lastSeen" to Timestamp.now()
                ))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error clearing FCM token on logout", e)
        }
        accountManager.setCurrentAccountEmail(null)
        authService.signOut()
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        val email = authService.currentUser?.email ?: throw Exception("Không tìm thấy email")
        authService.reauthenticate(email, oldPassword)
        authService.updatePassword(newPassword)
    }

    suspend fun forgotPassword(email: String) {
        authService.sendPasswordResetEmail(email)
    }

    suspend fun loginWithGoogle(idToken: String) {
        val firebaseUser = authService.signInWithGoogle(idToken)
        val userId = firebaseUser.uid

        // Check if user document exists (returning user) or needs to be created (new user)
        val existingUser = firestoreService.getUser(userId)
        if (existingUser == null) {
            // New Google user — create Firestore document
            val displayName = firebaseUser.displayName ?: ""
            val user = User(
                uid = userId,
                email = firebaseUser.email ?: "",
                username = displayName.lowercase().replace(" ", "_"),
                displayName = displayName,
                status = Constants.USER_STATUS_ACTIVE,
                avatarUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
            firestoreService.createUser(user)
        } else if (existingUser.status == Constants.USER_STATUS_PENDING_DELETION) {
            // Reactivate account
            firestoreService.updateUser(userId, mapOf(
                "status" to Constants.USER_STATUS_ACTIVE,
                "deletedAt" to FieldValue.delete()
            ))
        }

        // Save credential for account switching (no password for Google accounts)
        accountManager.addAccount(
            SavedAccount(
                email = firebaseUser.email ?: "",
                encryptedPassword = "",
                displayName = firebaseUser.displayName ?: existingUser?.displayName ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString() ?: existingUser?.avatarUrl ?: ""
            )
        )

        saveFcmToken()
    }

    suspend fun saveFcmToken() {
        try {
            val userId = authService.currentUserId ?: return
            val token = FirebaseMessaging.getInstance().token.await()
            firestoreService.updateUser(userId, mapOf("fcmToken" to token))
            Log.d("AuthRepository", "FCM token saved for user $userId")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to save FCM token", e)
        }
    }
}
