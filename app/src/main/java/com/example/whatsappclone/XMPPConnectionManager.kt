package com.example.whatsappclone

import android.content.Context
import android.util.Log
import com.example.whatsappclone.network.ApiResponse
import com.example.whatsappclone.network.ApiService
import com.google.firebase.messaging.FirebaseMessaging
import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.Chat
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.stringprep.XmppStringprepException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object XMPPConnectionManager {
    private var connection: AbstractXMPPConnection? = null
    private var chatManager: ChatManager? = null
    private var context: Context? = null
    private var currentUsername: String? = null
    private var currentServer: String? = null
    private var currentPassword: String? = null

    fun initialize(context: Context) {
        this.context = context
    }

    fun getConnection(): AbstractXMPPConnection? = connection

    suspend fun connect(server: String, username: String, password: String): Boolean {
        return try {
            // Store credentials for reconnection
            currentServer = server
            currentUsername = username
            currentPassword = password

            // Save credentials
            saveCredentials(server, username, password)

            // Test backend connectivity first
            testBackendConnection()

            // Build XMPP configuration
            val config = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(server)
                .setHost("10.102.126.8")
                .setPort(5222)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setSendPresence(true)
                .build()

            connection = XMPPTCPConnection(config)

            // Add connection listener
            connection?.addConnectionListener(object : ConnectionListener {
                override fun connected(conn: XMPPConnection?) {
                    Log.d("XMPP", "Connected to XMPP server")
                }

                override fun authenticated(conn: XMPPConnection?, resumed: Boolean) {
                    Log.d("XMPP", "Authenticated. Resumed: $resumed")

                    // Send FCM token and presence to backend
                    CoroutineScope(Dispatchers.IO).launch {
                        sendFCMTokenToBackend()
                        updateUserStatusOnBackend("online")
                        sendPresenceToServer()
                    }
                }

                override fun connectionClosed() {
                    Log.d("XMPP", "XMPP connection closed")

                    // Update backend about offline status
                    CoroutineScope(Dispatchers.IO).launch {
                        currentUsername?.let { username ->
                            updateUserStatusOnBackend("offline")
                        }
                    }
                }

                override fun connectionClosedOnError(e: Exception?) {
                    Log.e("XMPP", "XMPP connection closed on error: ${e?.message}")

                    // Update backend about offline status
                    CoroutineScope(Dispatchers.IO).launch {
                        currentUsername?.let { username ->
                            updateUserStatusOnBackend("offline")
                        }
                    }
                }
            })

            // Connect & login
            connection!!.connect()
            connection!!.login(username, password)

            // Enable auto-reconnect
            ReconnectionManager.getInstanceFor(connection).enableAutomaticReconnection()

            // Initialize chat manager
            chatManager = ChatManager.getInstanceFor(connection)

            // Retrieve offline messages
            retrieveOfflineMessages()

            true
        } catch (e: Exception) {
            Log.e("XMPP", "Connection failed", e)
            false
        }
    }

    private suspend fun testBackendConnection() {
        when (val response = ApiService.testConnection()) {
            is ApiResponse.Success -> {
                Log.d("Backend", "Backend is reachable: ${response.data}")
            }
            is ApiResponse.Error -> {
                Log.w("Backend", "Backend connection issue: ${response.message}")
            }
        }
    }

    private fun saveCredentials(server: String, username: String, password: String) {
        context?.let { ctx ->
            val sharedPrefs = ctx.getSharedPreferences("xmpp_credentials", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putString("server", server)
                .putString("username", username)
                .putString("password", password)
                .apply()
        }
    }

    private suspend fun sendFCMTokenToBackend() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d("FCM", "FCM Token obtained: $token")

                // Send token to local backend
                CoroutineScope(Dispatchers.IO).launch {
                    currentUsername?.let { username ->
                        when (val response = ApiService.registerFCMToken(username, token)) {
                            is ApiResponse.Success -> {
                                Log.d("FCM", "Token registered successfully with backend")

                                // Store token locally as well
                                context?.getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
                                    ?.edit()?.putString("token", token)?.apply()
                            }
                            is ApiResponse.Error -> {
                                Log.e("FCM", "Failed to register token with backend: ${response.message}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error in FCM token process", e)
        }
    }

    private suspend fun updateUserStatusOnBackend(status: String) {
        currentUsername?.let { username ->
            when (val response = ApiService.updateUserStatus(username, status)) {
                is ApiResponse.Success -> {
                    Log.d("Backend", "User status updated to $status")
                }
                is ApiResponse.Error -> {
                    Log.e("Backend", "Failed to update user status: ${response.message}")
                }
            }
        }
    }

    private fun sendPresenceToServer() {
        try {
            val presence = Presence(Presence.Type.available)
            presence.status = "online"
            connection?.sendStanza(presence)
            Log.d("XMPP", "Presence sent to XMPP server")
        } catch (e: Exception) {
            Log.e("XMPP", "Failed to send presence", e)
        }
    }

    private fun retrieveOfflineMessages() {
        context?.let { ctx ->
            val sharedPrefs = ctx.getSharedPreferences("offline_messages", Context.MODE_PRIVATE)
            val messages = sharedPrefs.getStringSet("messages", emptySet()) ?: emptySet()

            for (messageData in messages) {
                val parts = messageData.split("|")
                if (parts.size >= 3) {
                    val sender = parts[0]
                    val message = parts[1]
                    val timestamp = parts[2]

                    Log.d("XMPP", "Retrieved offline message from $sender: $message")
                }
            }

            // Clear offline messages after processing
            sharedPrefs.edit().remove("messages").apply()
        }
    }

    fun disconnect() {
        try {
            // Update backend about going offline
            CoroutineScope(Dispatchers.IO).launch {
                currentUsername?.let { username ->
                    updateUserStatusOnBackend("offline")
                }
            }

            // Send unavailable presence before disconnecting
            val presence = Presence(Presence.Type.unavailable)
            connection?.sendStanza(presence)

            connection?.disconnect()
            Log.d("XMPP", "Disconnected from XMPP server")
        } catch (e: Exception) {
            Log.e("XMPP", "Error during disconnect", e)
        } finally {
            connection = null
            chatManager = null
        }
    }

    fun isConnected(): Boolean {
        return connection?.isConnected ?: false
    }

    fun sendMessage(to: String, message: String) {
        try {
            val jid = JidCreate.entityBareFrom(to)
            val chat: Chat? = chatManager?.chatWith(jid)
            chat?.send(message)
            Log.d("XMPP", "Message sent to $to: $message")
        } catch (e: XmppStringprepException) {
            Log.e("XMPP", "Invalid JID", e)
        } catch (e: SmackException.NotConnectedException) {
            Log.e("XMPP", "Not connected", e)
        }
    }

    fun setMessageListener(listener: (from: String, message: String) -> Unit) {
        chatManager?.addIncomingListener { from, message, chat ->
            val sender = from.asEntityBareJidString()
            val body = message.body
            if (body != null) {
                Log.d("XMPP", "Message received from $sender: $body")
                listener(sender, body)
            }
        }
    }

    fun getChatManager(): ChatManager? = chatManager

    fun getCurrentUsername(): String? = currentUsername

    // Method to reconnect with stored credentials
    suspend fun reconnectWithStoredCredentials(): Boolean {
        context?.let { ctx ->
            val sharedPrefs = ctx.getSharedPreferences("xmpp_credentials", Context.MODE_PRIVATE)
            val server = sharedPrefs.getString("server", "") ?: ""
            val username = sharedPrefs.getString("username", "") ?: ""
            val password = sharedPrefs.getString("password", "") ?: ""

            if (server.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                Log.d("XMPP", "Attempting to reconnect with stored credentials for user: $username")
                return connect(server, username, password)
            }
        }
        return false
    }

    // Method to manually trigger wake up for testing
    suspend fun triggerWakeUpNotification(targetUsername: String) {
        when (val response = ApiService.sendWakeUpNotification(targetUsername)) {
            is ApiResponse.Success -> {
                Log.d("Backend", "Wake up notification sent to $targetUsername")
            }
            is ApiResponse.Error -> {
                Log.e("Backend", "Failed to send wake up: ${response.message}")
            }
        }
    }
}