package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cipher.crypto.EncryptedPayload
import com.example.cipher.crypto.EncryptionHelper
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class InboxActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)
        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)

        btnBack.setOnClickListener { finish() }

        rvMessages.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            if (userId == null) {
                tvEmptyState.visibility = View.VISIBLE
                rvMessages.visibility = View.GONE
                return@launch
            }

            try {
                val rows = SupabaseClientProvider.client.from("messages")
                    .select {
                        filter { eq("recipient_id", userId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<MessageRow>()

                val messages = rows.mapNotNull { row ->
                    val payload = EncryptedPayload(
                        encryptedAesKey = row.encrypted_aes_key,
                        iv = row.iv,
                        ciphertext = row.ciphertext
                    )
                    val decrypted = EncryptionHelper.decrypt(payload)
                        ?: "[Unable to decrypt — key may have expired]"

                    EncryptedMessage(
                        id = row.id,
                        senderId = row.sender_id,
                        decryptedPreview = decrypted,
                        receivedAt = formatTimestamp(row.created_at)
                    )
                }

                if (messages.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvMessages.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvMessages.visibility = View.VISIBLE
                    rvMessages.adapter = MessageAdapter(messages) { message ->
                        val intent = Intent(this@InboxActivity, ChatActivity::class.java).apply {
                            putExtra(ChatActivity.EXTRA_CONTACT_ID, message.senderId)
                            putExtra(ChatActivity.EXTRA_CONTACT_NAME, message.senderId.take(8) + "...")
                        }
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@InboxActivity, "Couldn't load your inbox. Try again.", Toast.LENGTH_SHORT).show()
                tvEmptyState.visibility = View.VISIBLE
                rvMessages.visibility = View.GONE
            }
        }
    }

    private fun formatTimestamp(isoTimestamp: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoTimestamp.substring(0, 19)) ?: return "Recently"
            val displayFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)
            displayFormat.format(date)
        } catch (e: Exception) {
            "Recently"
        }
    }
}