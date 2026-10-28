package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class ApiVaultItem(
    val id: String = "",
    val title: String = "",
    val category: String = "CREDENTIAL",
    val encryptedPayload: String = "",
    val iv: String = "",
    val securityLevel: String = "HIGH",
    val favorite: Boolean = false,
    val notesPreview: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = false)
data class CreateVaultItemRequest(
    val title: String,
    val category: String,
    val encryptedPayload: String,
    val iv: String,
    val securityLevel: String,
    val notesPreview: String? = null
)

@JsonClass(generateAdapter = false)
data class ApiAuditLog(
    val id: String = "",
    val eventType: String = "",
    val severity: String = "INFO",
    val details: String = "",
    val ipAddress: String = "127.0.0.1",
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = false)
data class CreateAuditLogRequest(
    val eventType: String,
    val severity: String,
    val details: String,
    val ipAddress: String = "127.0.0.1"
)

@JsonClass(generateAdapter = false)
data class ApiHealthResponse(
    val status: String = "HEALTHY",
    val serverTime: Long = System.currentTimeMillis(),
    val version: String = "v2.4-rest",
    val endpoint: String = "",
    val activeConnections: Int = 1
)

@JsonClass(generateAdapter = false)
data class ApiResponseStatus(
    val success: Boolean = true,
    val message: String = "Operation successful"
)

data class HttpTrafficLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val method: String, // "GET", "POST", "DELETE"
    val url: String,
    val statusCode: Int, // 200, 201, 404
    val durationMs: Long,
    val requestPayload: String? = null,
    val responsePayload: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
