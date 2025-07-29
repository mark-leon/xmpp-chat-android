
package com.example.whatsappclone


import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jivesoftware.smack.chat2.ChatManager
import com.example.whatsappclone.model.ChatMessage
import android.widget.Toast

class ChatActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var spinnerRecipients: Spinner
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var messagesAdapter: MessageAdapter

    private val messages = mutableListOf<ChatMessage>()
    private var currentRecipient: String? = null
    private val chatManager: ChatManager? = XMPPConnectionManager.getChatManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initializeViews()
        setupRecyclerView()
        setupRecipientSpinner()
        setupConnectionStatus()
        setupMessageListener()
        setupSendButton()
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
        // Get current user's domain
        val domain =  "localhost"

        // Create list of available recipients
        val currentUser = XMPPConnectionManager.getConnection()?.user?.asEntityBareJidString() ?: ""
        val recipients = listOf(
            "leion@$domain",
            "rafin@$domain"
        ).filter { it != currentUser } // Exclude current user

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
                // Clear messages when changing recipient
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
                // Only show messages from the current recipient
                if (from == currentRecipient) {
                    addMessage(ChatMessage(
                        sender = from.substringBefore("@"),
                        message = message,
                        isOutgoing = false
                    ))
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

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
//            XMPPConnectionManager.disconnect()
        }
    }
}



