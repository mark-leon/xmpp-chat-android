package com.example.whatsappclone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "chat_messages"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Register token with your server
        registerTokenWithServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Extract data from the message
        val jid = remoteMessage.data["jid"] ?: ""
        val sender = remoteMessage.data["sender"] ?: ""
        val body = remoteMessage.data["body"] ?: ""
        val type = remoteMessage.data["type"] ?: ""

        Log.d(TAG, "Message data - JID: $jid, Sender: $sender, Body: $body, Type: $type")

        // Show notification if app is in background or killed
        if (remoteMessage.notification != null) {
            showNotification(
                title = remoteMessage.notification!!.title ?: "New Message",
                body = remoteMessage.notification!!.body ?: "You have a new message",
                sender = sender,
                messageBody = body
            )
        } else {
            // Handle data-only message
            showNotification(
                title = "Message from ${sender.substringBefore("@")}",
                body = body,
                sender = sender,
                messageBody = body
            )
        }
    }

    private fun showNotification(title: String, body: String, sender: String, messageBody: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sender", sender)
            putExtra("message", messageBody)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Add this icon to your drawable
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for chat messages"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerTokenWithServer(token: String) {
        // Get current user JID from SharedPreferences or XMPPConnectionManager
        val currentUser = getCurrentUserJID()
        if (currentUser.isNotEmpty()) {
            FCMTokenManager.registerToken(currentUser, token)
        }
    }

    private fun getCurrentUserJID(): String {
        val sharedPref = getSharedPreferences("xmpp_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("current_user_jid", "") ?: ""
    }
}