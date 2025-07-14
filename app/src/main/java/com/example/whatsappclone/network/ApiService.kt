package com.example.whatsappclone.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ApiService {

    // Your local backend server URL
    private const val BASE_URL = "http://10.102.126.8:8800/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Register FCM token with backend server
     */
    suspend fun registerFCMToken(username: String, fcmToken: String): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("token", fcmToken)
                }

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/fcm/users/$username/token")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("ApiService", "FCM Token Registration - Response: $responseBody")

                if (response.isSuccessful) {
                    ApiResponse.Success(responseBody ?: "")
                } else {
                    ApiResponse.Error("Failed to register FCM token: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error registering FCM token", e)
                ApiResponse.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Update user online/offline status
     */
    suspend fun updateUserStatus(username: String, status: String): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("status", status)
                }

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/messages/users/$username/status")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("ApiService", "Status Update - Response: $responseBody")

                if (response.isSuccessful) {
                    ApiResponse.Success(responseBody ?: "")
                } else {
                    ApiResponse.Error("Failed to update status: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error updating user status", e)
                ApiResponse.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Send wake up notification to user
     */
    suspend fun sendWakeUpNotification(username: String): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/messages/users/$username/wake-up")
                    .post("".toRequestBody())
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("ApiService", "Wake Up Notification - Response: $responseBody")

                if (response.isSuccessful) {
                    ApiResponse.Success(responseBody ?: "")
                } else {
                    ApiResponse.Error("Failed to send wake up: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error sending wake up notification", e)
                ApiResponse.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Test backend connectivity
     */
    suspend fun testConnection(): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/health")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d("ApiService", "Health Check - Response: $responseBody")

                if (response.isSuccessful) {
                    ApiResponse.Success(responseBody ?: "")
                } else {
                    ApiResponse.Error("Backend not reachable: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error testing connection", e)
                ApiResponse.Error("Cannot connect to backend: ${e.message}")
            }
        }
    }

    /**
     * Get FCM token for a user (for testing purposes)
     */
    suspend fun getUserFCMToken(username: String): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/fcm/users/$username/token")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    ApiResponse.Success(responseBody ?: "")
                } else {
                    ApiResponse.Error("Failed to get token: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error getting FCM token", e)
                ApiResponse.Error("Network error: ${e.message}")
            }
        }
    }
}

/**
 * Sealed class for API responses
 */
sealed class ApiResponse {
    data class Success(val data: String) : ApiResponse()
    data class Error(val message: String) : ApiResponse()
}