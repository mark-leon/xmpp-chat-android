package com.example.whatsappclone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.whatsappclone.network.ApiResponse
import com.example.whatsappclone.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etServer: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnTestBackend: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBackendStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize XMPP manager with context
        XMPPConnectionManager.initialize(this)

        initializeViews()

        // Test backend connectivity on startup
        testBackendConnectivity()

        // Check if user is already logged in
        checkExistingLogin()
    }

    private fun initializeViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etServer = findViewById(R.id.etServer)
        btnLogin = findViewById(R.id.btnLogin)
        btnTestBackend = findViewById(R.id.btnTestBackend)
        progressBar = findViewById(R.id.progressBar)
        tvBackendStatus = findViewById(R.id.tvBackendStatus)

        btnLogin.setOnClickListener {
            performLogin()
        }

        btnTestBackend.setOnClickListener {
            testBackendConnectivity()
        }
    }

    private fun testBackendConnectivity() {
        tvBackendStatus.text = "Testing backend connection..."
        tvBackendStatus.setTextColor(getColor(android.R.color.darker_gray))

        CoroutineScope(Dispatchers.IO).launch {
            when (val response = ApiService.testConnection()) {
                is ApiResponse.Success -> {
                    withContext(Dispatchers.Main) {
                        tvBackendStatus.text = "✓ Backend Connected (localhost:8800)"
                        tvBackendStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                        Toast.makeText(this@LoginActivity, "Backend is reachable!", Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResponse.Error -> {
                    withContext(Dispatchers.Main) {
                        tvBackendStatus.text = "✗ Backend Error: ${response.message}"
                        tvBackendStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                        Toast.makeText(this@LoginActivity, "Backend connection failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun checkExistingLogin() {
        val sharedPrefs = getSharedPreferences("xmpp_credentials", MODE_PRIVATE)
        val server = sharedPrefs.getString("server", "")
        val username = sharedPrefs.getString("username", "")
        val password = sharedPrefs.getString("password", "")

        if (!server.isNullOrEmpty() && !username.isNullOrEmpty() && !password.isNullOrEmpty()) {
            // Pre-fill the form
            etServer.setText(server)
            etUsername.setText(username)
            etPassword.setText(password)

            Toast.makeText(this, "Found saved credentials. Click login to continue.", Toast.LENGTH_LONG).show()
        } else {
            // Set default values for testing
            etServer.setText("localhost")
            etUsername.setText("rafin") // or "leion"
            etPassword.setText("password")
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val server = etServer.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || server.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE
        btnLogin.isEnabled = false
        btnTestBackend.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First test backend connectivity
                when (val backendResponse = ApiService.testConnection()) {
                    is ApiResponse.Success -> {
                        // Backend is reachable, proceed with XMPP login
                        val xmppSuccess = XMPPConnectionManager.connect(server, username, password)

                        withContext(Dispatchers.Main) {
                            progressBar.visibility = ProgressBar.GONE
                            btnLogin.isEnabled = true
                            btnTestBackend.isEnabled = true

                            if (xmppSuccess) {
                                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()

                                // Start chat activity
                                startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "XMPP login failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    is ApiResponse.Error -> {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = ProgressBar.GONE
                            btnLogin.isEnabled = true
                            btnTestBackend.isEnabled = true

                            Toast.makeText(
                                this@LoginActivity,
                                "Backend not reachable. Please check if server is running on port 8800",
                                Toast.LENGTH_LONG
                            ).show()

                            // Update backend status
                            tvBackendStatus.text = "✗ Backend Error: ${backendResponse.message}"
                            tvBackendStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    btnLogin.isEnabled = true
                    btnTestBackend.isEnabled = true

                    Toast.makeText(this@LoginActivity, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't disconnect on destroy - let the connection persist
    }
}