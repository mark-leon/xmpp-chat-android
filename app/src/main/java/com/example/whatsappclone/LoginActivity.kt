//package com.example.whatsappclone
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ProgressBar
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.google.android.gms.tasks.OnCompleteListener
//import com.google.firebase.messaging.FirebaseMessaging
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//
//class LoginActivity : AppCompatActivity() {
//    private lateinit var etUsername: EditText
//    private lateinit var etPassword: EditText
//    private lateinit var etServer: EditText
//    private lateinit var btnLogin: Button
//    private lateinit var progressBar: ProgressBar
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_login)
//
//        etUsername = findViewById(R.id.etUsername)
//        etPassword = findViewById(R.id.etPassword)
//        etServer = findViewById(R.id.etServer)
//        btnLogin = findViewById(R.id.btnLogin)
//        progressBar = findViewById(R.id.progressBar)
//
//        // Check for stored credentials and auto-login
//        val creds = XMPPConnectionManager.getCredentials(this)
//        if (creds != null) {
//            val (server, username, password) = creds
//            attemptAutoLogin(server, username, password)
//            return
//        }
//
//        btnLogin.setOnClickListener {
//            val username = etUsername.text.toString().trim()
//            val password = etPassword.text.toString().trim()
//            val server = etServer.text.toString().trim()
//
//            if (username.isEmpty() || password.isEmpty() || server.isEmpty()) {
//                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            progressBar.visibility = ProgressBar.VISIBLE
//            btnLogin.isEnabled = false
//
//            CoroutineScope(Dispatchers.IO).launch {
//                val success = XMPPConnectionManager.connect(server, username, password)
//
//                withContext(Dispatchers.Main) {
//                    progressBar.visibility = ProgressBar.GONE
//                    btnLogin.isEnabled = true
//
//                    if (success) {
//                        XMPPConnectionManager.storeCredentials(this@LoginActivity, server, username, password)
//                        registerFcmToken(username)
//                        startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
//                        finish()
//                    } else {
//                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//    }
//
//    private fun attemptAutoLogin(server: String?, username: String?, password: String?) {
//        if (server.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty()) {
//            Toast.makeText(this, "Stored credentials are invalid", Toast.LENGTH_SHORT).show()
//            return
//        }
//        progressBar.visibility = ProgressBar.VISIBLE
//        CoroutineScope(Dispatchers.IO).launch {
//            val success = XMPPConnectionManager.connect(server, username, password)
//
//            withContext(Dispatchers.Main) {
//                progressBar.visibility = ProgressBar.GONE
//                if (success) {
//                    registerFcmToken(username)
//                    startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
//                    finish()
//                } else {
//                    Toast.makeText(this@LoginActivity, "Auto-login failed. Please login manually.", Toast.LENGTH_SHORT).show()
//                    // Optionally clear credentials: XMPPConnectionManager.clearCredentials(this@LoginActivity)
//                }
//            }
//        }
//    }
//
//    private fun registerFcmToken(username: String) {
//        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                return@OnCompleteListener
//            }
//            val token = task.result
//            // Send token to your backend server (e.g., https://push.example.com/register)
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    val client = OkHttpClient()
//                    val json = JSONObject().apply {
//                        put("username", username)
//                        put("token", token)
//                    }
//                    val body = json.toString().toRequestBody("application/json".toMediaType())
//                    val request = Request.Builder()
//                        .url("http://10.241.250.8:8800/register")  // Adjust endpoint as needed
//                        .post(body)
//                        .build()
//                    client.newCall(request).execute()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        })
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (isFinishing) {
//            // Optionally disconnect, but since we want resumption, perhaps don't
//            // XMPPConnectionManager.disconnect()
//        }
//    }
//}



//
//package com.example.whatsappclone
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ProgressBar
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.messaging.FirebaseMessaging
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//import org.jivesoftware.smack.packet.IQ
//
//
//class LoginActivity : AppCompatActivity() {
//    private lateinit var etUsername: EditText
//    private lateinit var etPassword: EditText
//    private lateinit var etServer: EditText
//    private lateinit var btnLogin: Button
//    private lateinit var progressBar: ProgressBar
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_login)
//        etUsername = findViewById(R.id.etUsername)
//        etPassword = findViewById(R.id.etPassword)
//        etServer = findViewById(R.id.etServer)
//        btnLogin = findViewById(R.id.btnLogin)
//        progressBar = findViewById(R.id.progressBar)
//        btnLogin.setOnClickListener {
//            val username = etUsername.text.toString().trim()
//            val password = etPassword.text.toString().trim()
//            val server = etServer.text.toString().trim()
//            if (username.isEmpty() || password.isEmpty() || server.isEmpty()) {
//                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//            progressBar.visibility = ProgressBar.VISIBLE
//            btnLogin.isEnabled = false
//            CoroutineScope(Dispatchers.IO).launch {
//                val success = XMPPConnectionManager.connect(server, username, password)
//                withContext(Dispatchers.Main) {
//                    progressBar.visibility = ProgressBar.GONE
//                    btnLogin.isEnabled = true
//                    if (success) {
//                        // Fetch FCM token and send IQ
//                        try {
//                            val token = FirebaseMessaging.getInstance().token.await()  // Async fetch
//                            val iq = FCMRegisterIQ(token)
//                            iq.type = IQ.Type.set
//                            XMPPConnectionManager.getConnection()?.sendIqRequestAndWaitForResponse(iq)  // Synchronous send for simplicity; use async if needed
//                        } catch (e: Exception) {
//                            e.printStackTrace()  // Handle token fetch/send failure (e.g., log or retry)
//                        }
//                    }
//                }
//            }
//        }
//    }
//    override fun onDestroy() {
//        super.onDestroy()
//        if (isFinishing) {
////            XMPPConnectionManager.disconnect()
//        }
//    }
//}


package com.example.whatsappclone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.jivesoftware.smackx.push_notifications.PushNotificationsManager
import org.jxmpp.jid.impl.JidCreate

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etServer: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    // Coroutine scope for the small extra work
    private val loginScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etServer   = findViewById(R.id.etServer)
        btnLogin   = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val server   = etServer.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || server.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE
        btnLogin.isEnabled = false

        // 1. XMPP login on IO thread
        CoroutineScope(Dispatchers.IO).launch {
            val ok = XMPPConnectionManager.connect(server, username, password)

            withContext(Dispatchers.Main) {
                progressBar.visibility = ProgressBar.GONE
                btnLogin.isEnabled = true

                if (ok) {
                    // 2. Register FCM token with fpush (fire-and-forget)
                    registerPushAsync()
                    // 3. Go to chat
                    startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Registers the Firebase Cloud-Messaging token with our fpush component
     * using Smack’s XEP-0357 PushNotificationsManager.
     */
    private fun registerPushAsync() = loginScope.launch {
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e("LoginActivity", "FCM token fetch failed", e)
            return@launch
        }

        val conn = XMPPConnectionManager.getConnection() ?: return@launch
        val bareJid = conn.user.asEntityBareJidString()

        // store for later usage (optional)
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putString("my_jid", bareJid)
            .apply()

        val pushJid = JidCreate.from("push.localhost")
        val node    = token
        val options = java.util.HashMap<String, String>()
        options["pushModule"] = "myAndroidApp"

        try {
            withContext(Dispatchers.IO) {
                PushNotificationsManager.getInstanceFor(conn)
                    .enable(pushJid, node, options)
            }
            Log.i("LoginActivity", "Push enabled with token=${token.take(8)}…")
        } catch (e: Exception) {
            Log.e("LoginActivity", "Push enable failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loginScope.cancel()
    }
}