package com.example.whatsappclone

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.example.whatsappclone.network.ApiResponse
import com.example.whatsappclone.network.ApiService
import android.widget.Toast
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

class ChatActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvBackendStatus: TextView
    private lateinit var spinnerRecipients: Spinner
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var messagesAdapter: MessageAdapter

    private val messages = mutableListOf<ChatMessage>()
    private var currentRecipient: String? = null
    private val chatManager: ChatManager? = XMPPConnectionManager.getChatManager()

    companion object {
        var isActivityVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize XMPP manager with context
        XMPPConnectionManager.initialize(this)

        initializeViews()
        setupRecyclerView()
        setupRecipientSpinner()
        setupConnectionStatus()
        setupMessageListener()
        setupSendButton()

        // Initialize FCM
        initializeFCM()

        // Check if we need to reconnect
        checkAndReconnectIfNeeded()

        // Check backend connectivity
        checkBackendConnectivity()

        // Handle notification intent
        handleNotificationIntent()
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true

        // Refresh connection status
        setupConnectionStatus()

        // Update user status to online
        updateUserStatusOnBackend("online")

        // Try to reconnect if disconnected
        if (!XMPPConnectionManager.isConnected()) {
            checkAndReconnectIfNeeded()
        }
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityVisible = false

        // Update user status to offline when closing app
        if (isFinishing) {
            updateUserStatusOnBackend("offline")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_test_backend -> {
                testBackendConnection()
                true
            }

            R.id.action_refresh_connection -> {
                checkAndReconnectIfNeeded()
                true
            }

            R.id.action_send_wake_up -> {
                sendTestWakeUp()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleNotificationIntent() {
        if (intent.getBooleanExtra("from_notification", false)) {
            val title = intent.getStringExtra("notification_title")
            val body = intent.getStringExtra("notification_body")

            Toast.makeText(this, "Opened from notification: $title", Toast.LENGTH_SHORT).show()
            Log.d("ChatActivity", "App opened from notification: $title - $body")
        }
    }

    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(this, "Failed to get FCM token", Toast.LENGTH_SHORT).show()
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "FCM Token: $token")

            // Store token locally
            val sharedPrefs = getSharedPreferences("fcm_token", MODE_PRIVATE)
            sharedPrefs.edit().putString("token", token).apply()

            // Send to backend
            sendFCMTokenToBackend(token)
        }
    }

    private fun sendFCMTokenToBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val username = XMPPConnectionManager.getCurrentUsername()
                if (username != null) {
                    when (val response = ApiService.registerFCMToken(username, token)) {
                        is ApiResponse.Success -> {
                            runOnUiThread {
                                Log.d("FCM", "FCM token registered with backend successfully")
                            }
                        }

                        is ApiResponse.Error -> {
                            runOnUiThread {
                                Log.e("FCM", "Failed to register FCM token: ${response.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error sending FCM token to backend", e)
            }
        }
    }

    private fun checkBackendConnectivity() {
        CoroutineScope(Dispatchers.IO).launch {
            when (val response = ApiService.testConnection()) {
                is ApiResponse.Success -> {
                    runOnUiThread {
                        tvBackendStatus.text = "Backend: Connected (Port 8800)"
                        tvBackendStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    }
                }

                is ApiResponse.Error -> {
                    runOnUiThread {
                        tvBackendStatus.text = "Backend: Disconnected (${response.message})"
                        tvBackendStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
                }
            }
        }
    }

    private fun testBackendConnection() {
        Toast.makeText(this, "Testing backend connection...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            when (val response = ApiService.testConnection()) {
                is ApiResponse.Success -> {
                    runOnUiThread {
                        Toast.makeText(
                            this@ChatActivity,
                            "Backend is reachable!",
                            Toast.LENGTH_SHORT
                        ).show()
                        checkBackendConnectivity()
                    }
                }

                is ApiResponse.Error -> {
                    runOnUiThread {
                        Toast.makeText(
                            this@ChatActivity,
                            "Backend error: ${response.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        checkBackendConnectivity()
                    }
                }
            }
        }
    }

    private fun sendTestWakeUp() {
        currentRecipient?.let { recipient ->
            val recipientUsername = recipient.substringBefore("@")

            CoroutineScope(Dispatchers.IO).launch {
                when (val response =
                    XMPPConnectionManager.triggerWakeUpNotification(recipientUsername)) {
                    is ApiResponse.Success -> {
                        runOnUiThread {
                            Toast.makeText(
                                this@ChatActivity,
                                "Wake up notification sent to $recipientUsername",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    is ApiResponse.Error -> {
                        runOnUiThread {
                            Toast.makeText(
                                this@ChatActivity,
                                "Failed to send wake up: ${response.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "Please select a recipient first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserStatusOnBackend(status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val username = XMPPConnectionManager.getCurrentUsername()
                if (username != null) {
                    ApiService.updateUserStatus(username, status)
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error updating user status", e)
            }
        }
    }

    private fun checkAndReconnectIfNeeded() {
        if (!XMPPConnectionManager.isConnected()) {
            Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                val success = XMPPConnectionManager.reconnectWithStoredCredentials()
                runOnUiThread {
                    if (success) {
                        setupConnectionStatus()
                        setupMessageListener()
                        Toast.makeText(
                            this@ChatActivity,
                            "Reconnected successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this@ChatActivity, "Failed to reconnect", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvBackendStatus = findViewById(R.id.tvBackendStatus)
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
        val recipients = listOf(
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
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                currentRecipient = recipients[position]
                messages.clear()
                messagesAdapter.notifyDataSetChanged()

                Toast.makeText(
                    this@ChatActivity,
                    "Chat with ${recipients[position]}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentRecipient = null
            }
        }
    }

    private fun setupConnectionStatus() {
        val xmppStatus = if (XMPPConnectionManager.isConnected()) {
            "XMPP: Connected (${XMPPConnectionManager.getConnection()?.user?.asEntityBareJidString()})"
        } else {
            "XMPP: Disconnected - Push notifications active"
        }

        tvStatus.text = xmppStatus
        tvStatus.setTextColor(
            if (XMPPConnectionManager.isConnected())
                getColor(android.R.color.holo_green_dark)
            else
                getColor(android.R.color.holo_orange_dark)
        )
    }

    private fun setupMessageListener() {
        XMPPConnectionManager.setMessageListener { from, message ->
            runOnUiThread {
                if (from == currentRecipient) {
                    addMessage(
                        ChatMessage(
                            sender = from.substringBefore("@"),
                            message = message,
                            isOutgoing = false
                        )
                    )
                }
            }
        }
    }

    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                if (currentRecipient == null) {
                    Toast.makeText(this, "Please select a recipient first", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Check if connected, if not try to reconnect
                        if (!XMPPConnectionManager.isConnected()) {
                            val reconnected = XMPPConnectionManager.reconnectWithStoredCredentials()
                            if (!reconnected) {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@ChatActivity,
                                        "Unable to send message: Not connected",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@launch
                            }
                        }

                        XMPPConnectionManager.sendMessage(currentRecipient!!, messageText)
                        runOnUiThread {
                            addMessage(
                                ChatMessage(
                                    sender = "Me",
                                    message = messageText,
                                    isOutgoing = true
                                )
                            )
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

}