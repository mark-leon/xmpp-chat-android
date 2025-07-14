package com.example.whatsappclone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.whatsappclone.network.ApiResponse
import com.example.whatsappclone.network.ApiService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "New FCM token received: $token")

        // Store token locally
        val sharedPrefs = getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("token", token).apply()

        // Send token to local backend server
        sendTokenToLocalBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "FCM message received from: ${remoteMessage.from}")
        Log.d("FCM", "Message data: ${remoteMessage.data}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            val messageType = remoteMessage.data["type"]

            when (messageType) {
                "chat_message" -> {
                    handleChatMessage(remoteMessage.data)
                }
                "wake_up" -> {
                    handleWakeUp(remoteMessage.data)
                }
                else -> {
                    Log.w("FCM", "Unknown message type: $messageType")
                }
            }
        }

        // Handle notification payload (if present)
        remoteMessage.notification?.let { notification ->
            Log.d("FCM", "Message Notification - Title: ${notification.title}, Body: ${notification.body}")
            showNotification(
                notification.title ?: "New Message",
                notification.body ?: ""
            )
        }
    }

    private fun handleChatMessage(data: Map<String, String>) {
        val sender = data["sender"] ?: "Unknown"
        val message = data["message"] ?: ""
        val timestamp = data["timestamp"] ?: System.currentTimeMillis().toString()
        val chatId = data["chat_id"] ?: ""

        Log.d("FCM", "Chat message from $sender: $message")

        // Show notification
        showNotification("New message from $sender", message)

        // Store message locally if app is in background
        if (!isAppInForeground()) {
            storeOfflineMessage(sender, message, timestamp)
            Log.d("FCM", "Message stored offline")
        } else {
            Log.d("FCM", "App is in foreground, message will be handled by XMPP")
        }

        // Try to re-establish XMPP connection if needed
        reconnectXMPPIfNeeded()
    }

    private fun handleWakeUp(data: Map<String, String>) {
        Log.d("FCM", "Wake up signal received")

        // Show a subtle notification
        showNotification("WhatsApp Clone", "You have new messages")

        // Re-establish XMPP connection
        reconnectXMPPIfNeeded()
    }

    private fun reconnectXMPPIfNeeded() {
        if (!XMPPConnectionManager.isConnected()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("FCM", "Attempting to reconnect XMPP...")
                    val success = XMPPConnectionManager.reconnectWithStoredCredentials()

                    if (success) {
                        Log.d("FCM", "XMPP reconnection successful")
                    } else {
                        Log.w("FCM", "XMPP reconnection failed")
                    }
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to reconnect XMPP", e)
                }
            }
        } else {
            Log.d("FCM", "XMPP already connected")
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open chat when notification is tapped
        val intent = Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
            putExtra("notification_title", title)
            putExtra("notification_body", body)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)

        Log.d("FCM", "Notification shown: $title - $body")
    }

    private fun sendTokenToLocalBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current username from stored credentials
                val sharedPrefs = getSharedPreferences("xmpp_credentials", Context.MODE_PRIVATE)
                val username = sharedPrefs.getString("username", "") ?: ""

                if (username.isNotEmpty()) {
                    when (val response = ApiService.registerFCMToken(username, token)) {
                        is ApiResponse.Success -> {
                            Log.d("FCM", "Token successfully sent to local backend")
                        }
                        is ApiResponse.Error -> {
                            Log.e("FCM", "Failed to send token to backend: ${response.message}")
                        }
                    }
                } else {
                    Log.w("FCM", "No username found, token will be sent later during login")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error sending token to local backend", e)
            }
        }
    }

    private fun isAppInForeground(): Boolean {
        return ChatActivity.isActivityVisible
    }

    private fun storeOfflineMessage(sender: String?, message: String?, timestamp: String?) {
        try {
            val sharedPrefs = getSharedPreferences("offline_messages", Context.MODE_PRIVATE)
            val existingMessages = sharedPrefs.getStringSet("messages", mutableSetOf()) ?: mutableSetOf()

            val messageData = "${sender ?: "Unknown"}|${message ?: ""}|${timestamp ?: System.currentTimeMillis()}"
            val updatedMessages = existingMessages.toMutableSet()
            updatedMessages.add(messageData)

            sharedPrefs.edit().putStringSet("messages", updatedMessages).apply()

            Log.d("FCM", "Offline message stored: $messageData")
        } catch (e: Exception) {
            Log.e("FCM", "Error storing offline message", e)
        }
    }

    companion object {
        private const val CHANNEL_ID = "chat_messages"
    }
}