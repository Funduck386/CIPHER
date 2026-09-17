package com.example.cipher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cipher.crypto.KeyManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Rotates in today's keypair and clears out yesterday's private key
        KeyManager.ensureTodayKeyPair()

        val tvPublicKey = findViewById<TextView>(R.id.tvPublicKey)
        val btnCopyKey = findViewById<LinearLayout>(R.id.btnCopyKey)
        val btnShareKey = findViewById<LinearLayout>(R.id.btnShareKey)
        val btnSendMessage = findViewById<LinearLayout>(R.id.btnSendMessage)
        val btnInbox = findViewById<LinearLayout>(R.id.btnInbox)

        val publicKey = KeyManager.getPublicKeyBase64() ?: "No key found"
        tvPublicKey.text = publicKey

        btnCopyKey.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Public Key", publicKey))
            Toast.makeText(this, "Public key copied", Toast.LENGTH_SHORT).show()
        }

        btnShareKey.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, publicKey)
            }
            startActivity(Intent.createChooser(shareIntent, "Share public key"))
        }

        btnSendMessage.setOnClickListener {
            startActivity(Intent(this, ComposeMessageActivity::class.java))
        }

        btnInbox.setOnClickListener {
            // TODO: startActivity(Intent(this, InboxActivity::class.java))
            Toast.makeText(this, "Inbox screen coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}