package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class VaultItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "CREDENTIAL", // "CREDENTIAL", "API_KEY", "SECURE_NOTE", "FINANCIAL", "RECOVERY"
    val encryptedPayload: String = "",
    val securityLevel: String = "HIGH", // "STANDARD", "HIGH", "MAXIMUM"
    val iv: String = "",
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val notesPreview: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventType: String = "VAULT_ACCESS", // "AUTH_LOGIN", "VAULT_ACCESS", "ITEM_ENCRYPTED", "ITEM_DECRYPTED", "PIN_VERIFIED", "PIN_FAILED", "SECURITY_SCAN", "BACKEND_PING"
    val severity: String = "INFO", // "INFO", "WARNING", "CRITICAL"
    val details: String = "",
    val ipAddress: String = "127.0.0.1",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SecurityProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val securityScore: Long = 95L,
    val twoFactorEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = true,
    val autoLockSeconds: Long = 60L,
    val backendStatus: String = "ONLINE",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DecryptedVaultSecret(
    val accountUsername: String = "",
    val secretPasswordOrKey: String = "",
    val websiteOrService: String = "",
    val notes: String = "",
    val customFields: Map<String, String> = emptyMap()
)
