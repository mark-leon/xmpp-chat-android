package com.example.whatsappclone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.chat2.ChatManager
import com.example.whatsappclone.model.ChatMessage

class ChatActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var spinnerRecipients: Spinner
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var messagesAdapter: MessageAdapter

    private val messages = mutableListOf<ChatMessage>()
    private var currentRecipient: String? = null
    private var chatManager: ChatManager? = XMPPConnectionManager.getChatManager()
    private lateinit var recipients: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initializeViews()
        setupRecyclerView()
        setupRecipientSpinner()
        // In onCreate, after setupRecipientSpinner()
        val recipientFromNotification = intent.getStringExtra("recipient")
        if (recipientFromNotification != null) {
            val adapter = spinnerRecipients.adapter as ArrayAdapter<String>
            val position = adapter.getPosition(recipientFromNotification)
            if (position >= 0) {
                spinnerRecipients.setSelection(position)
            } else {
                Toast.makeText(this, "Recipient not found", Toast.LENGTH_SHORT).show()
            }
        }

        // Check connection and reconnect if necessary
        if (!XMPPConnectionManager.isConnected()) {
            val creds = XMPPConnectionManager.getCredentials(this)
            if (creds == null) {
                Toast.makeText(this, "No credentials found. Redirecting to login.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
            val (server, username, password) = creds
            CoroutineScope(Dispatchers.IO).launch {
                val success = XMPPConnectionManager.connect(server, username, password)
                withContext(Dispatchers.Main) {
                    if (success) {
                        chatManager = XMPPConnectionManager.getChatManager()
                        setupConnectionStatus()
                        setupMessageListener()
                        setupSendButton()
                        handleIntentExtras()
                    } else {
                        Toast.makeText(this@ChatActivity, "Failed to reconnect. Redirecting to login.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ChatActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        } else {
            setupConnectionStatus()
            setupMessageListener()
            setupSendButton()
            handleIntentExtras()
        }
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        spinnerRecipients = findViewById(R.id.spinnerRecipients)
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

    private fun setupRecipientSpinner() {
        val domain = "localhost"
        val currentUser = XMPPConnectionManager.getConnection()?.user?.asEntityBareJidString() ?: ""
        recipients = listOf(
            "leion@$domain",
            "rafin@$domain"
        ).filter { it != currentUser }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            recipients
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRecipients.adapter = adapter

        spinnerRecipients.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentRecipient = recipients[position]
                // Clear messages when changing recipient (or load history if implemented)
                messages.clear()
                messagesAdapter.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentRecipient = null
            }
        }
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
                if (from == currentRecipient) {
                    addMessage(ChatMessage(
                        sender = from.substringBefore("@"),
                        message = message,
                        isOutgoing = false
                    ))
                } else {
                    // Optionally handle messages from other recipients (e.g., show in-app notification)
                }
            }
        }
    }

    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                if (currentRecipient == null) {
                    Toast.makeText(this, "Please select a recipient first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        XMPPConnectionManager.sendMessage(currentRecipient!!, messageText)
                        runOnUiThread {
                            addMessage(ChatMessage(
                                sender = "Me",
                                message = messageText,
                                isOutgoing = true
                            ))
                            etMessage.text.clear()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ChatActivity,
                                "Failed to send message: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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

    private fun handleIntentExtras() {
        val sender = intent.getStringExtra("sender")
        if (sender != null) {
            val pos = recipients.indexOf(sender)
            if (pos >= 0) {
                spinnerRecipients.setSelection(pos)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            // Optionally disconnect
            // XMPPConnectionManager.disconnect()
        }
    }
}