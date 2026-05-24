package com.example.nexus.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SavedAccount(
    val email: String,
    val encryptedPassword: String,
    val displayName: String,
    val avatarUrl: String
)

@Singleton
class AccountManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "nexus_accounts",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCOUNTS = "saved_accounts"
        private const val KEY_CURRENT_EMAIL = "current_account_email"
    }

    fun getSavedAccounts(): List<SavedAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SavedAccount>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addAccount(account: SavedAccount) {
        val accounts = getSavedAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.email == account.email }
        if (index >= 0) {
            accounts[index] = account
        } else {
            accounts.add(account)
        }
        prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts)).apply()
        prefs.edit().putString(KEY_CURRENT_EMAIL, account.email).apply()
    }

    fun removeAccount(email: String) {
        val accounts = getSavedAccounts().toMutableList()
        accounts.removeAll { it.email == email }
        prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts)).apply()
        if (getCurrentAccountEmail() == email) {
            prefs.edit().remove(KEY_CURRENT_EMAIL).apply()
        }
    }

    fun setCurrentAccountEmail(email: String?) {
        if (email != null) {
            prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
        } else {
            prefs.edit().remove(KEY_CURRENT_EMAIL).apply()
        }
    }

    fun getCurrentAccountEmail(): String? {
        return prefs.getString(KEY_CURRENT_EMAIL, null)
    }
}
