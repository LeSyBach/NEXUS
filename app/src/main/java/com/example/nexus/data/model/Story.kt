package com.example.nexus.data.model

import com.google.firebase.Timestamp

data class Story(
    val id: String = "",
    val userId: String = "",
    val content: String = "",
    val type: String = "text",
    val caption: String? = null,
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)
