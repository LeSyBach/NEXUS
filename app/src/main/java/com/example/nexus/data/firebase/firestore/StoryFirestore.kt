package com.example.nexus.data.firebase.firestore

import com.example.nexus.data.model.Story
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore operations for the `stories` collection.
 */
@Singleton
class StoryFirestore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createStory(story: Story): String {
        val docRef = firestore.collection("stories").add(story).await()
        return docRef.id
    }

    fun observeAllActiveStories(): Flow<List<Story>> = callbackFlow {
        val listener = firestore.collection("stories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                val stories = snapshot?.documents?.mapNotNull { doc ->
                    val story = doc.toObject(Story::class.java)?.copy(id = doc.id)
                    val expiresAt = story?.expiresAt?.toDate()?.time ?: 0L
                    if (expiresAt > now) story else null
                } ?: emptyList()
                trySend(stories)
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteStory(storyId: String) {
        firestore.collection("stories").document(storyId).delete().await()
    }

    suspend fun markStoryAsViewed(storyId: String, userId: String) {
        firestore.collection("stories").document(storyId)
            .update("viewedBy", FieldValue.arrayUnion(userId))
            .await()
    }

    suspend fun deleteUserStoriesByType(userId: String, type: String) {
        val snapshot = firestore.collection("stories")
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .get()
            .await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }
}
