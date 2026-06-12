package com.example.nexus.core.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getPinnedChatIdsFlow(userId: String): Flow<Set<String>> {
        val key = stringSetPreferencesKey("pinned_chats_$userId")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }
    }

    suspend fun toggleChatPin(userId: String, chatId: String) {
        val key = stringSetPreferencesKey("pinned_chats_$userId")
        context.dataStore.edit { preferences ->
            val currentPinned = preferences[key] ?: emptySet()
            if (currentPinned.contains(chatId)) {
                preferences[key] = currentPinned - chatId
            } else {
                if (currentPinned.size < 3) {
                    preferences[key] = currentPinned + chatId
                }
            }
        }
    }
}
