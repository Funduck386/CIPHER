package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ComposeMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_message)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val etRecipientKey = findViewById<EditText>(R.id.etRecipientKey)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        btnBack.setOnClickListener { finish() }

        btnSend.setOnClickListener {
            val recipientKey = etRecipientKey.text.toString().trim()
            val message = etMessage.text.toString().trim()

            if (recipientKey.isEmpty()) {
                Toast.makeText(this, "Enter the recipient's public key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (message.isEmpty()) {
                Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: encrypt `message` with recipientKey (EncryptionHelper),
            // then upload the ciphertext to Supabase tied to the recipient's current daily key
            startActivity(Intent(this, MessageSentActivity::class.java))
            finish()
        }
    }
}