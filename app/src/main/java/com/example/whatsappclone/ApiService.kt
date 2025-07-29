package com.example.whatsappclone

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterDeviceRequest(
    val jid: String,
    val fcm_token: String,
    val push_node: String?
)

data class RegisterDeviceResponse(
    val success: Boolean,
    val message: String,
    val registered_at: String?
)

interface ApiService {
    @POST("register")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): Response<RegisterDeviceResponse>

    companion object {
        private const val BASE_URL = "http://10.102.126.8:8080/"

        val instance: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}