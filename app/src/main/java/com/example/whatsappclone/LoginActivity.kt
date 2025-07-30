

package com.example.whatsappclone
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
                        startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
//            XMPPConnectionManager.disconnect()
        }
    }
}