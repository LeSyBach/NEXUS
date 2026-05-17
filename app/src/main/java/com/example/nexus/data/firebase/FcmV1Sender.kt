package com.example.nexus.data.firebase

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.nexus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.TimeUnit

/**
 * Gửi FCM trực tiếp qua HTTP v1 API (không cần Cloud Functions).
 *
 * Flow: JWT (RS256) → OAuth2 access token → POST fcm.googleapis.com/v1/.../messages:send
 * Token được cache trong SharedPreferences (50 phút).
 */
class FcmV1Sender(private val context: Context) {

    companion object {
        private const val TAG = "FcmV1Sender"
        private const val FCM_ENDPOINT = "https://fcm.googleapis.com/v1/projects/%s/messages:send"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
        private const val TOKEN_CACHE_DURATION_MS = 50 * 60 * 1000L // 50 phút

        private const val PREFS_NAME = "fcm_v1_prefs"
        private const val KEY_CACHED_TOKEN = "cached_access_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry_time"
    }

    private val projectId = BuildConfig.FCM_SA_PROJECT_ID
    private val clientEmail = BuildConfig.FCM_SA_CLIENT_EMAIL
    private val privateKeyId = BuildConfig.FCM_SA_PRIVATE_KEY_ID
    // Private key đọc từ assets (do Gradle task writeFcmKey ghi từ local.properties)
    private val privateKeyBase64: String by lazy {
        context.assets.open("fcm_sa_key.txt").bufferedReader().use { it.readText().trim() }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val fcmUrl = String.format(FCM_ENDPOINT, projectId)

    /**
     * Gửi FCM data-only message đến 1 thiết bị.
     */
    suspend fun send(token: String, data: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken() ?: return@withContext false

            val message = buildDataOnlyMessage(token, data)
            val body = JSONObject().put("message", message).toString()

            val request = Request.Builder()
                .url(fcmUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM sent OK → ${response.body?.string()}")
                    true
                } else {
                    val errorBody = response.body?.string() ?: "no body"
                    Log.e(TAG, "FCM send failed [${response.code}]: $errorBody")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FCM send error", e)
            false
        }
    }

    // ════════════════════════════════════════════════════════════════
    // OAuth2 ACCESS TOKEN
    // ════════════════════════════════════════════════════════════════

    private fun getAccessToken(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedToken = prefs.getString(KEY_CACHED_TOKEN, null)
        val expiryTime = prefs.getLong(KEY_TOKEN_EXPIRY, 0)

        if (cachedToken != null && System.currentTimeMillis() < expiryTime) {
            return cachedToken
        }

        return fetchNewAccessToken(prefs)
    }

    private fun fetchNewAccessToken(prefs: android.content.SharedPreferences): String? {
        return try {
            val jwt = createSignedJwt()
            val assertion = "urn:ietf:params:oauth:grant-type:jwt-bearer"

            val formBody = "grant_type=$assertion&assertion=$jwt"
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Token exchange failed [${response.code}]: ${response.body?.string()}")
                    return null
                }

                val json = JSONObject(response.body?.string() ?: "{}")
                val accessToken = json.getString("access_token")
                val expiresIn = json.getLong("expires_in") // seconds

                // Cache token
                prefs.edit()
                    .putString(KEY_CACHED_TOKEN, accessToken)
                    .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + expiresIn * 1000 - 60_000)
                    .apply()

                Log.d(TAG, "OAuth2 token obtained, expires in ${expiresIn}s")
                accessToken
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get OAuth2 token", e)
            null
        }
    }

    // ════════════════════════════════════════════════════════════════
    // JWT CREATION (RS256)
    // ════════════════════════════════════════════════════════════════

    private fun createSignedJwt(): String {
        val now = System.currentTimeMillis() / 1000

        val header = JSONObject()
            .put("alg", "RS256")
            .put("typ", "JWT")
            .put("kid", privateKeyId)
            .toString()

        val payload = JSONObject()
            .put("iss", clientEmail)
            .put("scope", SCOPE)
            .put("aud", TOKEN_URL)
            .put("iat", now)
            .put("exp", now + 3600)
            .toString()

        val headerB64 = base64UrlEncode(header.toByteArray())
        val payloadB64 = base64UrlEncode(payload.toByteArray())
        val toSign = "$headerB64.$payloadB64"

        val signature = signWithRSA(toSign.toByteArray())
        val signatureB64 = base64UrlEncode(signature)

        return "$toSign.$signatureB64"
    }

    private fun signWithRSA(data: ByteArray): ByteArray {
        val keyBytes = Base64.decode(privateKeyBase64, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    private fun base64UrlEncode(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    // ════════════════════════════════════════════════════════════════
    // FCM MESSAGE BUILDER (data-only, giống Cloud Function cũ)
    // ════════════════════════════════════════════════════════════════

    private fun buildDataOnlyMessage(token: String, data: Map<String, String>): JSONObject {
        val dataJson = JSONObject()
        data.forEach { (key, value) -> dataJson.put(key, value) }
        dataJson.put("sentAt", System.currentTimeMillis().toString())

        return JSONObject()
            .put("token", token)
            .put("data", dataJson)
            .put("android", JSONObject().put("priority", "high"))
    }
}
