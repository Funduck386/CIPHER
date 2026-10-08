package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cipher.crypto.EncryptionHelper
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

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

            btnSend.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Find which user this public key currently belongs to.
                    val match = SupabaseClientProvider.client.from("public_keys")
                        .select(columns = Columns.list("user_id")) {
                            filter { eq("public_key", recipientKey) }
                        }
                        .decodeSingleOrNull<PublicKeyOwner>()

                    if (match == null) {
                        Toast.makeText(
                            this@ComposeMessageActivity,
                            "No user found with that public key. Ask them to request/share their current key.",
                            Toast.LENGTH_LONG
                        ).show()
                        btnSend.isEnabled = true
                        return@launch
                    }

                    val senderId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (senderId == null) {
                        Toast.makeText(this@ComposeMessageActivity, "You're not signed in", Toast.LENGTH_SHORT).show()
                        btnSend.isEnabled = true
                        return@launch
                    }

                    // Encrypt locally, then upload only the ciphertext.
                    val payload = EncryptionHelper.encrypt(message, recipientKey)

                    SupabaseClientProvider.client.from("messages").insert(
                        MessageInsert(
                            sender_id = senderId,
                            recipient_id = match.user_id,
                            encrypted_aes_key = payload.encryptedAesKey,
                            iv = payload.iv,
                            ciphertext = payload.ciphertext
                        )
                    )

                    startActivity(Intent(this@ComposeMessageActivity, MessageSentActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ComposeMessageActivity, "Couldn't send the message. Try again.", Toast.LENGTH_SHORT).show()
                    btnSend.isEnabled = true
                }
            }
        }
    }
}