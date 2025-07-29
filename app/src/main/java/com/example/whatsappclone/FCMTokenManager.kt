package com.example.whatsappclone

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FCMTokenManager {
    private const val TAG = "FCMTokenManager"
    private const val SERVER_URL = "http://10.102.126.8:8080" // Your server URL

    fun initializeFCM(context: Context, userJID: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM Token obtained: $token")

                // Save current user JID for later use
                saveCurrentUserJID(context, userJID)

                // Register token with server
                registerToken(userJID, token)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
            }
        }
    }

    fun registerToken(jid: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiService.instance.registerDevice(
                    RegisterDeviceRequest(
                        jid = jid,
                        fcm_token = token,
                        push_node = null // Optional
                    )
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Token registered successfully")
                } else {
                    Log.e(TAG, "Failed to register token: ${response.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error registering token", e)
            }
        }
    }

    private fun saveCurrentUserJID(context: Context, jid: String) {
        val sharedPref = context.getSharedPreferences("xmpp_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("current_user_jid", jid).apply()
    }
}