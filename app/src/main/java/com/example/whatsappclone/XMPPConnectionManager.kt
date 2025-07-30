package com.example.whatsappclone

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.*
import org.jivesoftware.smack.tcp.*

import org.jivesoftware.smackx.push_notifications.PushNotificationsManager
import org.jxmpp.jid.impl.*
import org.jxmpp.jid.parts.Resourcepart
import org.jxmpp.stringprep.*

object XMPPConnectionManager {
    private var connection: AbstractXMPPConnection? = null
    private var chatManager: ChatManager? = null

    fun getConnection(): AbstractXMPPConnection? = connection
    fun getChatManager(): ChatManager? = chatManager

    suspend fun connect(server: String, username: String, password: String): Boolean {
        return try {
            // Configure connection
            val config = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(server)
                .setHost("10.102.126.8")
                .setPort(5222)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .build()

            // Create connection
            connection = XMPPTCPConnection(config)
            connection!!.connect()
            connection!!.login(username, password)

            chatManager = ChatManager.getInstanceFor(connection)

            registerPushNotifications()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun registerPushNotifications() {
        try {
            // Assuming 'connection' is an initialized XMPPConnection object
            val pushManager = PushNotificationsManager.getInstanceFor(connection)
            val node = JidCreate.bareFrom("pubsub.localhost/push")
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            println("fcc,${fcmToken}")

            // Corrected: Create a HashMap instead of a Kotlin Map
            val publishOptions = HashMap<String, String>()
            publishOptions["device_id"] = fcmToken
            publishOptions["service"] = "fcm"

            pushManager.enable(node, Resourcepart.from("mobile").toString(), publishOptions)
            println("Push notification registered with node: ${node.asBareJid()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            // Disable push notifications before disconnecting
            val pushManager = PushNotificationsManager.getInstanceFor(connection)
            val node = JidCreate.bareFrom("pubsub.localhost/push")
//            pushManager.disable(node)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
}