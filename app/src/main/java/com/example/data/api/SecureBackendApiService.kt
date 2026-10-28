package com.example.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SecureBackendApiService {

    @GET("health")
    suspend fun checkHealth(): ApiHealthResponse

    @GET("vault/items")
    suspend fun getVaultItems(
        @Header("Authorization") authHeader: String
    ): List<ApiVaultItem>

    @POST("vault/items")
    suspend fun createVaultItem(
        @Header("Authorization") authHeader: String,
        @Body request: CreateVaultItemRequest
    ): ApiVaultItem

    @DELETE("vault/items/{id}")
    suspend fun deleteVaultItem(
        @Header("Authorization") authHeader: String,
        @Path("id") itemId: String
    ): ApiResponseStatus

    @POST("vault/items/{id}/favorite")
    suspend fun toggleFavorite(
        @Header("Authorization") authHeader: String,
        @Path("id") itemId: String
    ): ApiResponseStatus

    @GET("audit/logs")
    suspend fun getAuditLogs(
        @Header("Authorization") authHeader: String
    ): List<ApiAuditLog>

    @POST("audit/logs")
    suspend fun createAuditLog(
        @Header("Authorization") authHeader: String,
        @Body request: CreateAuditLogRequest
    ): ApiResponseStatus
}
