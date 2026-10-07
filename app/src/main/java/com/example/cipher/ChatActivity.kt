package com.example.cipher

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    // TODO: load the real contact from local storage / Supabase using contactId from the intent
    private lateinit var contact: Contact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID) ?: ""
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Contact"
        val contactKey = intent.getStringExtra(EXTRA_CONTACT_KEY) ?: ""
        val contactKeyDate = intent.getStringExtra(EXTRA_CONTACT_KEY_DATE) ?: ""

        contact = Contact(contactId, contactName, contactKey, contactKeyDate)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvContactName = findViewById<TextView>(R.id.tvContactName)
        val tvKeyStatus = findViewById<TextView>(R.id.tvKeyStatus)
        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val lockedBanner = findViewById<android.widget.LinearLayout>(R.id.lockedBanner)
        val inputBar = findViewById<android.widget.LinearLayout>(R.id.inputBar)
        val etNewKey = findViewById<EditText>(R.id.etNewKey)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)
        val btnRequestKey = findViewById<android.widget.LinearLayout>(R.id.btnRequestKey)
        val etChatInput = findViewById<EditText>(R.id.etChatInput)
        val btnSendChat = findViewById<TextView>(R.id.btnSendChat)

        tvContactName.text = contact.displayName

        // TODO: replace with real message history fetched/decrypted for this contact
        val messages = mutableListOf<ChatMessage>()
        val adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        fun refreshLockState() {
            val today = dateFormat.format(Date())
            val isUnlocked = contact.keyDate == today && contact.publicKeyBase64.isNotEmpty()

            if (isUnlocked) {
                lockedBanner.visibility = View.GONE
                inputBar.visibility = View.VISIBLE
                tvKeyStatus.text = "Key up to date"
                tvKeyStatus.setTextColor(0xFF6FCF97.toInt())
            } else {
                lockedBanner.visibility = View.VISIBLE
                inputBar.visibility = View.GONE
                tvKeyStatus.text = "Key expired — unlock to continue"
                tvKeyStatus.setTextColor(0xFFE8C97A.toInt())
            }
        }

        refreshLockState()

        btnBack.setOnClickListener { finish() }

        btnRequestKey.setOnClickListener {
            // TODO: create a key_request row in Supabase: { from: myUserId, to: contact.id }
            Toast.makeText(this, "Key request sent to ${contact.displayName}", Toast.LENGTH_SHORT).show()
        }

        btnUnlock.setOnClickListener {
            val newKey = etNewKey.text.toString().trim()
            if (newKey.isEmpty()) {
                Toast.makeText(this, "Paste the contact's current public key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO: validate the key format, then persist it (local DB / Supabase) tied to this contact
            contact = contact.copy(publicKeyBase64 = newKey, keyDate = dateFormat.format(Date()))
            etNewKey.text.clear()
            refreshLockState()
            Toast.makeText(this, "Chat unlocked", Toast.LENGTH_SHORT).show()
        }

        btnSendChat.setOnClickListener {
            val text = etChatInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            // TODO: encrypt `text` with contact.publicKeyBase64 (EncryptionHelper) and upload to Supabase
            val newMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                text = text,
                isSentByMe = true,
                timestamp = timeFormat.format(Date())
            )
            messages.add(newMessage)
            adapter.notifyItemInserted(messages.size - 1)
            rvChat.scrollToPosition(messages.size - 1)
            etChatInput.text.clear()
        }
    }

    companion object {
        const val EXTRA_CONTACT_ID = "extra_contact_id"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_CONTACT_KEY = "extra_contact_key"
        const val EXTRA_CONTACT_KEY_DATE = "extra_contact_key_date"
    }
}