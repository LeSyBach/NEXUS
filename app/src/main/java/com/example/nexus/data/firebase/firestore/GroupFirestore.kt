package com.example.nexus.data.firebase.firestore

import com.example.nexus.core.utils.Constants
import com.example.nexus.data.model.Chat
import com.example.nexus.data.model.Group
import com.example.nexus.data.model.GroupMember
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore operations for the `groups` collection and group-related chat operations.
 */
@Singleton
class GroupFirestore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createGroup(group: Group): String {
        val docRef = firestore.collection(Constants.COLLECTION_GROUPS)
            .add(group)
            .await()
        return docRef.id
    }

    suspend fun getGroup(groupId: String): Group? {
        return firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .get()
            .await()
            .toObject(Group::class.java)
    }

    suspend fun updateGroup(groupId: String, updates: Map<String, Any>) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update(updates)
            .await()
    }

    suspend fun addGroupMember(groupId: String, member: GroupMember) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", FieldValue.arrayUnion(member))
            .await()
    }

    suspend fun removeGroupMember(groupId: String, member: GroupMember) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", FieldValue.arrayRemove(member))
            .await()
    }

    suspend fun createGroupChat(chat: Chat, group: Group): String {
        val chatRef = firestore.collection(Constants.COLLECTION_CHATS).document()
        val groupRef = firestore.collection(Constants.COLLECTION_GROUPS).document()
        val chatWithId = chat.copy(id = chatRef.id)
        val groupWithId = group.copy(id = groupRef.id, chatId = chatRef.id)
        val batch = firestore.batch()
        batch.set(chatRef, chatWithId)
        batch.set(groupRef, groupWithId)
        batch.commit().await()
        return chatRef.id
    }

    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Group::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun promoteGroupMember(groupId: String, chatId: String, userId: String) {
        val group = getGroup(groupId) ?: return
        val updatedMembers = group.members.map { member ->
            if (member.userId == userId) member.copy(role = Constants.ROLE_ADMIN) else member
        }
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", updatedMembers)
            .await()
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("adminIds", FieldValue.arrayUnion(userId))
            .await()
    }

    suspend fun demoteGroupMember(groupId: String, chatId: String, userId: String) {
        val group = getGroup(groupId) ?: return
        val updatedMembers = group.members.map { member ->
            if (member.userId == userId) member.copy(role = Constants.ROLE_MEMBER) else member
        }
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .update("members", updatedMembers)
            .await()
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update("adminIds", FieldValue.arrayRemove(userId))
            .await()
    }

    suspend fun removeGroupMemberByKick(groupId: String, chatId: String, member: GroupMember) {
        val group = getGroup(groupId)
        if (group != null) {
            val updatedMembers = group.members.filter { it.userId != member.userId }
            firestore.collection(Constants.COLLECTION_GROUPS)
                .document(groupId)
                .update("members", updatedMembers)
                .await()
        }
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(
                mapOf(
                    "participants" to FieldValue.arrayRemove(member.userId),
                    "adminIds" to FieldValue.arrayRemove(member.userId),
                    "updatedAt" to Timestamp.now()
                )
            )
            .await()
    }

    suspend fun dissolveGroup(chatId: String, groupId: String) {
        firestore.collection(Constants.COLLECTION_GROUPS)
            .document(groupId)
            .delete()
            .await()
        firestore.collection(Constants.COLLECTION_CHATS)
            .document(chatId)
            .update(
                mapOf(
                    "participants" to emptyList<String>(),
                    "updatedAt" to Timestamp.now()
                )
            )
            .await()
    }
}
