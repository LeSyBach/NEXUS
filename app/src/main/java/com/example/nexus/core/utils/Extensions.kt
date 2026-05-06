package com.example.nexus.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.util.UUID

/**
 * Kotlin extension functions used across the NEXUS app.
 */

// ── String Extensions ──

fun String.capitalizeFirst(): String {
    return this.replaceFirstChar { it.uppercase() }
}

fun String.initials(): String {
    return this.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
}

// ── Context Extensions ──

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.getFileName(uri: Uri): String? {
    var name: String? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

fun Context.getFileSize(uri: Uri): Long {
    var size: Long = 0
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex >= 0 && cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
        }
    }
    return size
}

// ── Flow Extensions ──

fun <T> Flow<T>.handleErrors(onError: (Throwable) -> Unit): Flow<T> {
    return this.catch { e -> onError(e) }
}

// ── General Extensions ──

fun generateId(): String = UUID.randomUUID().toString()

fun Long.toReadableFileSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$this B"
    }
}
