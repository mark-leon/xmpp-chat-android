package com.example.whatsappclone

import org.jivesoftware.smack.*
import org.jivesoftware.smack.chat2.*
import org.jivesoftware.smack.tcp.*
import org.jivesoftware.smack.roster.*
import org.jivesoftware.smack.packet.*
import org.jxmpp.jid.*
import org.jxmpp.jid.impl.*
import org.jxmpp.stringprep.*

object XMPPConnectionManager {
    private var connection: AbstractXMPPConnection? = null
    private var chatManager: ChatManager? = null

    fun getConnection(): AbstractXMPPConnection? = connection

    suspend fun connect(server: String, username: String, password: String): Boolean {
        return try {
            // Configure connection
            val config = XMPPTCPConnectionConfiguration.builder()
                .setXmppDomain(server)
                .setHost("10.123.162.8")
                .setPort(5222)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .build()

            // Create connection
            connection = XMPPTCPConnection(config)
            connection!!.connect()
            connection!!.login(username, password)

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
}