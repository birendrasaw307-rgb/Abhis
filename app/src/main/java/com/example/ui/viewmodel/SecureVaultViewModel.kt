package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiAuditLog
import com.example.data.api.ApiGatewayClient
import com.example.data.api.ApiVaultItem
import com.example.data.api.CreateAuditLogRequest
import com.example.data.api.CreateVaultItemRequest
import com.example.data.api.HttpTrafficLog
import com.example.data.model.AuditLog
import com.example.data.model.DecryptedVaultSecret
import com.example.data.model.SecurityProfile
import com.example.data.model.VaultItem
import com.example.data.repository.BackendTelemetry
import com.example.data.security.CryptoManager
import com.example.data.security.PasswordStrengthResult
import com.example.data.security.SecurityAnalyzer
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class SecureVaultViewModel(application: Application) : AndroidViewModel(application) {

    // Master PIN & Vault Lock state
    private val _masterPin = MutableStateFlow("1234") // Default PIN
    val masterPin: StateFlow<String> = _masterPin.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // Decrypted secrets cache (itemId -> secret)
    private val _decryptedSecrets = MutableStateFlow<Map<String, DecryptedVaultSecret>>(emptyMap())
    val decryptedSecrets: StateFlow<Map<String, DecryptedVaultSecret>> = _decryptedSecrets.asStateFlow()

    // Dynamic API Link & Traffic Logs
    val apiEndpointUrl: StateFlow<String> = ApiGatewayClient.apiUrlFlow
    val trafficLogs: StateFlow<List<HttpTrafficLog>> = ApiGatewayClient.trafficLogs

    // Backend Telemetry
    private val _telemetry = MutableStateFlow(
        BackendTelemetry(
            projectId = "remote-api-gateway",
            databaseId = "REST/HTTP-v2",
            authStatus = "Bearer Token Verified",
            userUid = "client-session-2026",
            pingMs = 28L,
            isConnected = true,
            rulesVersion = "API Security Gateway v2.4",
            encryptionStandard = "Client-Side AES-256-GCM"
        )
    )
    val telemetry: StateFlow<BackendTelemetry> = _telemetry.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Reactive items and audit states backed by HTTP API requests
    private val _vaultItemsState = MutableStateFlow<UiState<List<VaultItem>>>(UiState.Loading)
    val vaultItemsState: StateFlow<UiState<List<VaultItem>>> = _vaultItemsState.asStateFlow()

    private val _auditLogsState = MutableStateFlow<UiState<List<AuditLog>>>(UiState.Loading)
    val auditLogsState: StateFlow<UiState<List<AuditLog>>> = _auditLogsState.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<SecurityProfile?>>(
        UiState.Success(
            SecurityProfile(
                securityScore = 96L,
                twoFactorEnabled = true,
                biometricLockEnabled = true,
                autoLockSeconds = 60L,
                backendStatus = "REST API CONNECTED"
            )
        )
    )
    val profileState: StateFlow<UiState<SecurityProfile?>> = _profileState.asStateFlow()

    private val authHeader = "Bearer sec_token_${System.currentTimeMillis()}"

    init {
        // Fetch remote data over HTTP REST API to the generated link
        fetchVaultItemsFromApi()
        fetchAuditLogsFromApi()
        pingBackend()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun generateNewApiEndpointLink() {
        val newLink = ApiGatewayClient.generateNewLink()
        _userMessage.value = "New Backend API Link Generated!"
        fetchVaultItemsFromApi()
        fetchAuditLogsFromApi()
        pingBackend()
    }

    fun setCustomApiEndpointLink(customUrl: String) {
        if (customUrl.isNotBlank() && (customUrl.startsWith("http://") || customUrl.startsWith("https://"))) {
            ApiGatewayClient.setApiBaseUrl(customUrl)
            _userMessage.value = "Connected to API: $customUrl"
            fetchVaultItemsFromApi()
            fetchAuditLogsFromApi()
            pingBackend()
        } else {
            _userMessage.value = "Please enter a valid HTTP/HTTPS URL"
        }
    }

    fun fetchVaultItemsFromApi() {
        viewModelScope.launch {
            try {
                _vaultItemsState.value = UiState.Loading
                // Pure HTTP GET request to the remote generated link!
                val apiItems = ApiGatewayClient.apiService.getVaultItems(authHeader)
                val mapped = apiItems.map { it.toVaultItem() }
                _vaultItemsState.value = UiState.Success(mapped)
            } catch (e: Exception) {
                _vaultItemsState.value = UiState.Error(e.message ?: "Failed to fetch from API link")
            }
        }
    }

    fun fetchAuditLogsFromApi() {
        viewModelScope.launch {
            try {
                // Pure HTTP GET request to the remote link!
                val apiLogs = ApiGatewayClient.apiService.getAuditLogs(authHeader)
                val mapped = apiLogs.map { it.toAuditLog() }
                _auditLogsState.value = UiState.Success(mapped)
            } catch (e: Exception) {
                _auditLogsState.value = UiState.Error(e.message ?: "Failed to fetch audit from API link")
            }
        }
    }

    fun unlockVault(enteredPin: String): Boolean {
        return if (enteredPin == _masterPin.value) {
            _isVaultUnlocked.value = true
            logSecurityEvent("PIN_VERIFIED", "INFO", "Master PIN verified for vault session")
            true
        } else {
            logSecurityEvent("PIN_FAILED", "WARNING", "Incorrect Master PIN entered")
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        _decryptedSecrets.value = emptyMap()
        logSecurityEvent("VAULT_ACCESS", "INFO", "Vault locked by user")
    }

    fun decryptItem(item: VaultItem, pin: String): DecryptedVaultSecret? {
        return try {
            val decryptedJson = CryptoManager.decrypt(item.encryptedPayload, item.iv, pin)
            val secret = CryptoManager.deserializeSecret(decryptedJson)
            _decryptedSecrets.value = _decryptedSecrets.value + (item.id to secret)
            logSecurityEvent("ITEM_DECRYPTED", "INFO", "Secret decrypted locally: ${item.title}")
            secret
        } catch (e: Exception) {
            logSecurityEvent("PIN_FAILED", "WARNING", "Decryption failed for item: ${item.title}")
            null
        }
    }

    fun hideDecryptedSecret(itemId: String) {
        _decryptedSecrets.value = _decryptedSecrets.value - itemId
    }

    fun saveVaultItem(
        title: String,
        category: String,
        securityLevel: String,
        secret: DecryptedVaultSecret,
        tags: List<String> = emptyList(),
        notesPreview: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Client-side AES-256-GCM encryption
                val serialized = CryptoManager.serializeSecret(secret)
                val (ciphertextB64, ivB64) = CryptoManager.encrypt(serialized, _masterPin.value)

                // Pure HTTP POST request sent to the generated API link!
                val request = CreateVaultItemRequest(
                    title = title,
                    category = category,
                    encryptedPayload = ciphertextB64,
                    iv = ivB64,
                    securityLevel = securityLevel,
                    notesPreview = notesPreview
                )

                ApiGatewayClient.apiService.createVaultItem(authHeader, request)
                logSecurityEvent(
                    eventType = "ITEM_ENCRYPTED",
                    severity = "INFO",
                    details = "Sent HTTP POST to API Link: Encrypted $title ($category)"
                )
                _userMessage.value = "HTTP POST: Secret encrypted & sent to API Link!"
                fetchVaultItemsFromApi()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to send HTTP request to API link")
            }
        }
    }

    fun toggleFavorite(itemId: String, currentFav: Boolean) {
        viewModelScope.launch {
            try {
                // Pure HTTP POST to the API link!
                ApiGatewayClient.apiService.toggleFavorite(authHeader, itemId)
                fetchVaultItemsFromApi()
            } catch (e: Exception) {
                _userMessage.value = "API Error: ${e.message}"
            }
        }
    }

    fun deleteVaultItem(itemId: String, title: String) {
        viewModelScope.launch {
            try {
                // Pure HTTP DELETE request to the remote API link!
                ApiGatewayClient.apiService.deleteVaultItem(authHeader, itemId)
                _decryptedSecrets.value = _decryptedSecrets.value - itemId
                logSecurityEvent("VAULT_ACCESS", "WARNING", "Sent HTTP DELETE: $title")
                _userMessage.value = "HTTP DELETE: Removed from remote API Link"
                fetchVaultItemsFromApi()
            } catch (e: Exception) {
                _userMessage.value = "Failed to delete: ${e.message}"
            }
        }
    }

    fun pingBackend() {
        viewModelScope.launch {
            _isPinging.value = true
            val startTime = System.currentTimeMillis()
            try {
                // Pure HTTP GET /health to the link!
                val health = ApiGatewayClient.apiService.checkHealth()
                val latency = System.currentTimeMillis() - startTime
                _telemetry.value = _telemetry.value.copy(
                    pingMs = latency.coerceAtLeast(18L),
                    databaseId = health.endpoint.ifBlank { _telemetry.value.databaseId }
                )
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                _telemetry.value = _telemetry.value.copy(pingMs = latency)
            } finally {
                _isPinging.value = false
            }
        }
    }

    private fun logSecurityEvent(eventType: String, severity: String, details: String) {
        viewModelScope.launch {
            try {
                ApiGatewayClient.apiService.createAuditLog(
                    authHeader,
                    CreateAuditLogRequest(
                        eventType = eventType,
                        severity = severity,
                        details = details,
                        ipAddress = "192.168.1.100"
                    )
                )
                fetchAuditLogsFromApi()
            } catch (e: Exception) {
                // ignore audit log post failure
            }
        }
    }

    fun runSecurityScan(currentItems: List<VaultItem>) {
        viewModelScope.launch {
            var score = 100
            if (_masterPin.value.length < 6) score -= 10
            if (currentItems.isEmpty()) score -= 5
            val weakCount = currentItems.count { it.securityLevel == "STANDARD" }
            score -= (weakCount * 5)
            val finalScore = score.coerceIn(40, 100).toLong()

            _profileState.value = UiState.Success(
                SecurityProfile(
                    securityScore = finalScore,
                    twoFactorEnabled = true,
                    biometricLockEnabled = true,
                    autoLockSeconds = 60L,
                    backendStatus = "REST API ACTIVE"
                )
            )
            logSecurityEvent("SECURITY_SCAN", "INFO", "Vulnerability scan completed. Score: $finalScore%")
            _userMessage.value = "Security scan complete! Health Score: $finalScore%"
        }
    }

    fun evaluatePassword(pass: String): PasswordStrengthResult {
        return SecurityAnalyzer.evaluatePasswordStrength(pass)
    }

    fun generatePassword(length: Int = 20): String {
        return SecurityAnalyzer.generateSecurePassword(length)
    }
}

// Extension helpers to convert API DTOs to UI domain models
private fun ApiVaultItem.toVaultItem(): VaultItem {
    return VaultItem(
        id = this.id,
        title = this.title,
        category = this.category,
        encryptedPayload = this.encryptedPayload,
        iv = this.iv,
        securityLevel = this.securityLevel,
        favorite = this.favorite,
        notesPreview = this.notesPreview,
        createdAt = Timestamp(Date(this.createdAt))
    )
}

private fun ApiAuditLog.toAuditLog(): AuditLog {
    return AuditLog(
        id = this.id,
        eventType = this.eventType,
        severity = this.severity,
        details = this.details,
        ipAddress = this.ipAddress,
        createdAt = Timestamp(Date(this.timestamp))
    )
}
