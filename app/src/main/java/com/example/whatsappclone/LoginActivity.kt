package com.example.whatsappclone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etServer: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etServer = findViewById(R.id.etServer)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        // Check for stored credentials and auto-login
        val creds = XMPPConnectionManager.getCredentials(this)
        if (creds != null) {
            val (server, username, password) = creds
            attemptAutoLogin(server, username, password)
            return
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val server = etServer.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || server.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = ProgressBar.VISIBLE
            btnLogin.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                val success = XMPPConnectionManager.connect(server, username, password)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    btnLogin.isEnabled = true

                    if (success) {
                        XMPPConnectionManager.storeCredentials(this@LoginActivity, server, username, password)
                        registerFcmToken(username)
                        startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun attemptAutoLogin(server: String?, username: String?, password: String?) {
        if (server.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty()) {
            Toast.makeText(this, "Stored credentials are invalid", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val success = XMPPConnectionManager.connect(server, username, password)

            withContext(Dispatchers.Main) {
                progressBar.visibility = ProgressBar.GONE
                if (success) {
                    registerFcmToken(username)
                    startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Auto-login failed. Please login manually.", Toast.LENGTH_SHORT).show()
                    // Optionally clear credentials: XMPPConnectionManager.clearCredentials(this@LoginActivity)
                }
            }
        }
    }

    private fun registerFcmToken(username: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            val token = task.result
            // Send token to your backend server (e.g., https://push.example.com/register)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val client = OkHttpClient()
                    val json = JSONObject().apply {
                        put("username", username)
                        put("token", token)
                    }
                    val body = json.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url("http://10.230.214.8:8800/register")  // Adjust endpoint as needed
                        .post(body)
                        .build()
                    client.newCall(request).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            // Optionally disconnect, but since we want resumption, perhaps don't
            // XMPPConnectionManager.disconnect()
        }
    }
}