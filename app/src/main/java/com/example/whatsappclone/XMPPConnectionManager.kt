package com.example.whatsappclone

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.*
import org.jivesoftware.smack.tcp.*
import org.jivesoftware.smack.packet.*
import org.jxmpp.jid.*
import org.jxmpp.jid.impl.*
import org.jxmpp.stringprep.*


import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.StandardExtensionElement
import org.jivesoftware.smackx.iqregister.packet.Registration
import org.jivesoftware.smackx.sid.element.OriginIdElement
import org.jxmpp.jid.impl.JidCreate
import org.jivesoftware.smack.packet.SimpleIQ
import org.jivesoftware.smackx.push_notifications.PushNotificationsManager
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.jivesoftware.smackx.xdata.FormField
import org.jxmpp.jid.EntityBareJid
import com.google.firebase.messaging.FirebaseMessaging


object XMPPConnectionManager {
    private var connection: AbstractXMPPConnection? = null
    private var chatManager: ChatManager? = null

    fun getConnection(): AbstractXMPPConnection? = connection

    suspend fun connect(server: String?, username: String?, password: String?): Boolean {
        return try {
            // Configure connection
            val config = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(server)
                .setHost("10.212.78.8")
                .setPort(5222)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .build()

            // Create connection
            connection = XMPPTCPConnection(config)
            connection!!.connect()
            connection!!.login(username, password)

            // Enable automatic reconnection
            ReconnectionManager.getInstanceFor(connection).enableAutomaticReconnection()

            // Initialize chat manager
            chatManager = ChatManager.getInstanceFor(connection)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    fun disconnect() {
        connection?.disconnect()
        connection = null
        chatManager = null
    }

    fun isConnected(): Boolean {
        return connection?.isConnected ?: false
    }

    fun sendMessage(to: String, message: String) {
        try {
            val jid = JidCreate.entityBareFrom(to)
            val chat = chatManager?.chatWith(jid)
            chat?.send(message)
        } catch (e: XmppStringprepException) {
            e.printStackTrace()
        }
    }

    fun setMessageListener(listener: (from: String, message: String) -> Unit) {
        chatManager?.addIncomingListener { from, message, chat ->
            val sender = from.asEntityBareJidString()
            val body = message.body
            if (body != null) {
                listener(sender, body)
            }
        }
    }

    fun getChatManager(): ChatManager? = chatManager

    // Helper to store credentials securely (using EncryptedSharedPreferences)
    fun storeCredentials(context: Context, server: String, username: String, password: String) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs: SharedPreferences = EncryptedSharedPreferences.create(
            context,
            "xmpp_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        prefs.edit().apply {
            putString("server", server)
            putString("username", username)
            putString("password", password)
            apply()
        }
    }

    // Helper to get credentials
    fun getCredentials(context: Context): Triple<String?, String?, String?>? {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs: SharedPreferences = EncryptedSharedPreferences.create(
            context,
            "xmpp_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val server = prefs.getString("server", null)
        val username = prefs.getString("username", null)
        val password = prefs.getString("password", null)

        if (server != null && username != null && password != null) {
            return Triple(server, username, password)
        }
        return null
    }

    // Helper to clear credentials
    fun clearCredentials(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs: SharedPreferences = EncryptedSharedPreferences.create(
            context,
            "xmpp_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        prefs.edit().clear().apply()
    }


//    private suspend fun uploadFcmToken(connection: AbstractXMPPConnection) {
//        val prefs = MyApp.instance.getSharedPreferences("prefs", Context.MODE_PRIVATE)
//        val token = prefs.getString("fcm_token", null) ?: return
//
//        val ext = StandardExtensionElement.builder("register", "https://fcm.googleapis.com/fcm")
//            .addElement("token", token)
//            .build()
//
//        val iq = SimpleIQ("register", "https://fcm.googleapis.com/fcm")
//        iq.type = IQ.Type.set
//        iq.addExtension(ext)
//
//
//    }
}