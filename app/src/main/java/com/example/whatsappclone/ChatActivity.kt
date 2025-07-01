
package com.example.whatsappclone


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jivesoftware.smack.chat2.ChatManager
import com.example.whatsappclone.model.ChatMessage

class ChatActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var messagesAdapter: MessageAdapter

    private val messages = mutableListOf<ChatMessage>()
    private val chatManager: ChatManager? = XMPPConnectionManager.getChatManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initializeViews()
        setupRecyclerView()
        setupConnectionStatus()
        setupMessageListener()
        setupSendButton()
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messagesAdapter
    }

    private fun setupConnectionStatus() {
        tvStatus.text = if (XMPPConnectionManager.isConnected()) {
            "Status: Connected to ${XMPPConnectionManager.getConnection()?.user?.asEntityBareJidString()}"
        } else {
            "Status: Disconnected"
        }
    }

    private fun setupMessageListener() {
        XMPPConnectionManager.setMessageListener { from, message ->
            runOnUiThread {
                addMessage(ChatMessage(
                    sender = from,
                    message = message,
                    isOutgoing = false
                ))
            }
        }
    }

    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                // For simplicity, we'll hardcode the recipient
                // In a real app, you'd select a contact to chat with
                val userJid = XMPPConnectionManager.getConnection()?.user?.asEntityBareJidString()
                val domain = userJid?.substringAfter("@") ?: "example.com"
                val recipient = "recipient@$domain" // Replace with actual recipient in real app

                CoroutineScope(Dispatchers.IO).launch {
                    XMPPConnectionManager.sendMessage(recipient, messageText)
                    runOnUiThread {
                        addMessage(ChatMessage(
                            sender = "Me",
                            message = messageText,
                            isOutgoing = true
                        ))
                        etMessage.text.clear()
                    }
                }
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        messagesAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            XMPPConnectionManager.disconnect()
        }
    }
}




