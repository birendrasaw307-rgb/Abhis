package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.model.SecurityProfile
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

class SecurityProfileRepository(private val db: FirebaseFirestore) {

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

    fun observeProfile(): Flow<SecurityProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid/security_profile/main"
        emitAll(
            db.collection("users").document(uid).collection("security_profile").document("main")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toObject(SecurityProfile::class.java)
                    else null
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveProfile(profile: SecurityProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("security_profile").document("main")
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "securityScore" to profile.securityScore,
            "twoFactorEnabled" to profile.twoFactorEnabled,
            "biometricLockEnabled" to profile.biometricLockEnabled,
            "autoLockSeconds" to profile.autoLockSeconds,
            "backendStatus" to profile.backendStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
