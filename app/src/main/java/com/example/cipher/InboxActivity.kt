package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InboxActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)
        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)

        btnBack.setOnClickListener { finish() }

        // TODO: replace with real senders fetched from Supabase for today's public key
        val messages = emptyList<EncryptedMessage>()

        if (messages.isEmpty()) {
            tvEmptyState.visibility = android.view.View.VISIBLE
            rvMessages.visibility = android.view.View.GONE
        } else {
            tvEmptyState.visibility = android.view.View.GONE
            rvMessages.visibility = android.view.View.VISIBLE
            rvMessages.layoutManager = LinearLayoutManager(this)
            rvMessages.adapter = MessageAdapter(messages) { message ->
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONTACT_ID, message.id)
                    putExtra(ChatActivity.EXTRA_CONTACT_NAME, message.senderPublicKeyBase64.take(12) + "...")
                    putExtra(ChatActivity.EXTRA_CONTACT_KEY, message.senderPublicKeyBase64)
                    // TODO: pass the actual date this contact's key was confirmed, not today's
                    putExtra(ChatActivity.EXTRA_CONTACT_KEY_DATE, message.receivedAt)
                }
                startActivity(intent)
            }
        }
    }
}