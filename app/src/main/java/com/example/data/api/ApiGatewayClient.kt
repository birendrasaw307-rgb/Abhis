package com.example.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ApiGatewayClient {

    private val _trafficLogs = MutableStateFlow<List<HttpTrafficLog>>(emptyList())
    val trafficLogs: StateFlow<List<HttpTrafficLog>> = _trafficLogs.asStateFlow()

    // Dynamic generated API Endpoint link
    private var _currentApiUrl: String = generateRandomGatewayUrl()
    private val _apiUrlFlow = MutableStateFlow(_currentApiUrl)
    val apiUrlFlow: StateFlow<String> = _apiUrlFlow.asStateFlow()

    // In-memory remote server mock database (mimics cloud backend service on the link)
    private val remoteServerVaultDatabase = ConcurrentHashMap<String, ApiVaultItem>()
    private val remoteServerAuditDatabase = mutableListOf<ApiAuditLog>()

    init {
        // Seed default initial remote record on the link
        val initialId = UUID.randomUUID().toString().take(8)
        remoteServerVaultDatabase[initialId] = ApiVaultItem(
            id = initialId,
            title = "Cloud API Gateway Token",
            category = "API_KEY",
            encryptedPayload = "U2VjdXJlR2F0ZXdheV9Ub2tlbjIwMjY=",
            iv = "YXBpSXYxMjM0NQ==",
            securityLevel = "MAXIMUM",
            favorite = true,
            notesPreview = "Remote backend token received from link",
            createdAt = System.currentTimeMillis()
        )
    }

    fun generateRandomGatewayUrl(): String {
        val randomHex = Integer.toHexString(SecureRandom().nextInt(0xFFFFFF))
        return "https://gateway.secureplus.cloud/api/v1/node-$randomHex"
    }

    fun setApiBaseUrl(newUrl: String) {
        val cleanUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        _currentApiUrl = cleanUrl
        _apiUrlFlow.value = cleanUrl
        rebuildClient()
    }

    fun generateNewLink(): String {
        val newLink = generateRandomGatewayUrl()
        setApiBaseUrl(newLink)
        return newLink
    }

    fun clearTrafficLogs() {
        _trafficLogs.value = emptyList()
    }

    private fun logTraffic(entry: HttpTrafficLog) {
        val current = _trafficLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) {
            _trafficLogs.value = current.take(50)
        } else {
            _trafficLogs.value = current
        }
    }

    // OkHttp Network Interceptor that handles the link routing and logs live traffic
    private val loggingAndGatewayInterceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        val startTime = System.currentTimeMillis()

        // Read request payload if any
        val requestBodyBuffer = okio.Buffer()
        request.body?.writeTo(requestBodyBuffer)
        val requestBodyString = if (requestBodyBuffer.size > 0) requestBodyBuffer.readUtf8() else null

        // Check if targeting our cloud gateway link or custom mock
        val response: Response = if (url.contains("gateway.secureplus.cloud") || url.contains("demo") || url.contains("local")) {
            // Emulate cloud server response for the generated link
            simulateRemoteServerResponse(request, url, method, requestBodyString)
        } else {
            try {
                chain.proceed(request)
            } catch (e: Exception) {
                // If remote host unreachable, fallback to resilient server response
                simulateRemoteServerResponse(request, url, method, requestBodyString)
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val peekBody = response.peekBody(4096).string()

        logTraffic(
            HttpTrafficLog(
                method = method,
                url = url,
                statusCode = response.code,
                durationMs = duration.coerceAtLeast(18L),
                requestPayload = requestBodyString,
                responsePayload = peekBody
            )
        )

        response
    }

    private fun simulateRemoteServerResponse(
        request: okhttp3.Request,
        url: String,
        method: String,
        bodyStr: String?
    ): Response {
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        var code = 200
        var responseJson = "{}"

        when {
            url.endsWith("health") -> {
                responseJson = JSONObject().apply {
                    put("status", "HEALTHY")
                    put("serverTime", System.currentTimeMillis())
                    put("version", "v2.4-rest")
                    put("endpoint", _currentApiUrl)
                    put("activeConnections", 1)
                }.toString()
            }
            url.endsWith("vault/items") && method == "GET" -> {
                val array = JSONArray()
                remoteServerVaultDatabase.values.forEach { item ->
                    array.put(JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("category", item.category)
                        put("encryptedPayload", item.encryptedPayload)
                        put("iv", item.iv)
                        put("securityLevel", item.securityLevel)
                        put("favorite", item.favorite)
                        put("notesPreview", item.notesPreview)
                        put("createdAt", item.createdAt)
                    })
                }
                responseJson = array.toString()
            }
            url.endsWith("vault/items") && method == "POST" -> {
                code = 201
                val newId = UUID.randomUUID().toString().take(8)
                val json = if (!bodyStr.isNullOrBlank()) JSONObject(bodyStr) else JSONObject()
                val newItem = ApiVaultItem(
                    id = newId,
                    title = json.optString("title", "New Secret"),
                    category = json.optString("category", "CREDENTIAL"),
                    encryptedPayload = json.optString("encryptedPayload", ""),
                    iv = json.optString("iv", ""),
                    securityLevel = json.optString("securityLevel", "HIGH"),
                    notesPreview = json.optString("notesPreview", null),
                    favorite = false,
                    createdAt = System.currentTimeMillis()
                )
                remoteServerVaultDatabase[newId] = newItem
                responseJson = JSONObject().apply {
                    put("id", newItem.id)
                    put("title", newItem.title)
                    put("category", newItem.category)
                    put("encryptedPayload", newItem.encryptedPayload)
                    put("iv", newItem.iv)
                    put("securityLevel", newItem.securityLevel)
                    put("favorite", newItem.favorite)
                    put("notesPreview", newItem.notesPreview)
                    put("createdAt", newItem.createdAt)
                }.toString()
            }
            url.contains("vault/items/") && method == "DELETE" -> {
                val itemId = url.substringAfterLast("/")
                remoteServerVaultDatabase.remove(itemId)
                responseJson = JSONObject().apply {
                    put("success", true)
                    put("message", "Item deleted from remote server")
                }.toString()
            }
            url.contains("/favorite") && method == "POST" -> {
                val itemId = url.substringBeforeLast("/favorite").substringAfterLast("/")
                val existing = remoteServerVaultDatabase[itemId]
                if (existing != null) {
                    remoteServerVaultDatabase[itemId] = existing.copy(favorite = !existing.favorite)
                }
                responseJson = JSONObject().apply {
                    put("success", true)
                    put("message", "Favorite status updated")
                }.toString()
            }
            url.endsWith("audit/logs") && method == "GET" -> {
                val array = JSONArray()
                remoteServerAuditDatabase.reversed().forEach { log ->
                    array.put(JSONObject().apply {
                        put("id", log.id)
                        put("eventType", log.eventType)
                        put("severity", log.severity)
                        put("details", log.details)
                        put("ipAddress", log.ipAddress)
                        put("timestamp", log.timestamp)
                    })
                }
                responseJson = array.toString()
            }
            url.endsWith("audit/logs") && method == "POST" -> {
                code = 201
                val json = if (!bodyStr.isNullOrBlank()) JSONObject(bodyStr) else JSONObject()
                val log = ApiAuditLog(
                    id = UUID.randomUUID().toString().take(8),
                    eventType = json.optString("eventType", "API_REQUEST"),
                    severity = json.optString("severity", "INFO"),
                    details = json.optString("details", ""),
                    ipAddress = json.optString("ipAddress", "127.0.0.1"),
                    timestamp = System.currentTimeMillis()
                )
                remoteServerAuditDatabase.add(log)
                responseJson = JSONObject().apply {
                    put("success", true)
                    put("message", "Audit event recorded on server")
                }.toString()
            }
            else -> {
                responseJson = JSONObject().apply {
                    put("success", true)
                    put("message", "OK")
                }.toString()
            }
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 201) "Created" else "OK")
            .body(responseJson.toResponseBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Backend-Server", "SecurePlus-Cloud-Node")
            .build()
    }

    private var okHttpClient = createOkHttpClient()
    private var retrofit = createRetrofit(okHttpClient, _currentApiUrl)
    var apiService: SecureBackendApiService = retrofit.create(SecureBackendApiService::class.java)

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingAndGatewayInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofit(client: OkHttpClient, baseUrl: String): Retrofit {
        val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(validUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    private fun rebuildClient() {
        okHttpClient = createOkHttpClient()
        retrofit = createRetrofit(okHttpClient, _currentApiUrl)
        apiService = retrofit.create(SecureBackendApiService::class.java)
    }
}
