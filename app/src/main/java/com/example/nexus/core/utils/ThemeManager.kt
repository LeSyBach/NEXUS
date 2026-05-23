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
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme")
    }

    val isDarkModeFlow: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE]
    }

    val useSystemThemeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_SYSTEM_THEME] ?: true
    }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDarkMode
            preferences[USE_SYSTEM_THEME] = false
        }
    }

    suspend fun setUseSystemTheme(useSystem: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SYSTEM_THEME] = useSystem
        }
    }
}
