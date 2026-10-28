package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.model.DecryptedVaultSecret
import com.example.data.model.VaultItem
import com.example.data.security.CryptoManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class VaultRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeVaultItems(): Flow<List<VaultItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/vault_items"
        emitAll(
            db.collection("users").document(uid).collection("vault_items")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(VaultItem::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveVaultItem(
        title: String,
        category: String,
        securityLevel: String,
        secret: DecryptedVaultSecret,
        masterPin: String,
        tags: List<String> = emptyList(),
        notesPreview: String? = null
    ): String {
        val uid = requireUserId()
        val serialized = CryptoManager.serializeSecret(secret)
        val (ciphertextB64, ivB64) = CryptoManager.encrypt(serialized, masterPin)

        val docRef = db.collection("users").document(uid).collection("vault_items").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to title,
            "category" to category,
            "encryptedPayload" to ciphertextB64,
            "securityLevel" to securityLevel,
            "iv" to ivB64,
            "tags" to tags,
            "favorite" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!notesPreview.isNullOrBlank()) {
            payload["notesPreview"] = notesPreview.take(100)
        }

        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateVaultItem(
        item: VaultItem,
        newSecret: DecryptedVaultSecret?,
        masterPin: String
    ) {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "Item ID must not be blank" }
        requireNotNull(item.createdAt) { "Item createdAt must not be null" }

        val docRef = db.collection("users").document(uid).collection("vault_items").document(item.id)

        val updatedItem = if (newSecret != null) {
            val serialized = CryptoManager.serializeSecret(newSecret)
            val (ciphertextB64, ivB64) = CryptoManager.encrypt(serialized, masterPin)
            item.copy(
                encryptedPayload = ciphertextB64,
                iv = ivB64,
                updatedAt = null // server timestamp will populate
            )
        } else {
            item.copy(updatedAt = null)
        }

        try {
            docRef.set(updatedItem).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFavorite(itemId: String, isFavorite: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("vault_items").document(itemId)
        try {
            docRef.update(
                mapOf(
                    "favorite" to isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteVaultItem(itemId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("vault_items").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
