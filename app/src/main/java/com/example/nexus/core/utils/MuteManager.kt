package com.example.nexus.core.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MuteManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun muteKey(chatId: String) = booleanPreferencesKey("mute_$chatId")

    fun isMutedFlow(chatId: String): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[muteKey(chatId)] ?: false
        }
    }

    suspend fun setMuted(chatId: String, muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[muteKey(chatId)] = muted
        }
    }
}
