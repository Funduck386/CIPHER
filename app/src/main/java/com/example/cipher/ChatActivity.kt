package com.example.cipher

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

class ChatActivity : AppCompatActivity() {

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    private var contactId: String = ""
    private var contactName: String = "Contact"
    private var contactPublicKey: String? = null
    private var contactKeyDate: String? = null

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    private lateinit var tvKeyStatus: TextView
    private lateinit var lockedBanner: LinearLayout
    private lateinit var inputBar: LinearLayout
    private lateinit var rvChat: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactId = intent.getStringExtra(EXTRA_CONTACT_ID) ?: ""
        contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Contact"

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvContactName = findViewById<TextView>(R.id.tvContactName)
        tvKeyStatus = findViewById(R.id.tvKeyStatus)
        rvChat = findViewById(R.id.rvChat)
        lockedBanner = findViewById(R.id.lockedBanner)
        inputBar = findViewById(R.id.inputBar)
        val etNewKey = findViewById<EditText>(R.id.etNewKey)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)
        val btnRequestKey = findViewById<LinearLayout>(R.id.btnRequestKey)
        val etChatInput = findViewById<EditText>(R.id.etChatInput)
        val btnSendChat = findViewById<TextView>(R.id.btnSendChat)

        tvContactName.text = contactName

        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnRequestKey.setOnClickListener {
            lifecycleScope.launch {
                val myId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@launch
                try {
                    SupabaseClientProvider.client.from("key_requests").insert(
                        mapOf("from_user_id" to myId, "to_user_id" to contactId)
                    )
                    Toast.makeText(this@ChatActivity, "Key request sent to $contactName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ChatActivity, "Couldn't send the request. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnUnlock.setOnClickListener {
            val newKey = etNewKey.text.toString().trim()
            if (newKey.isEmpty()) {
                Toast.makeText(this, "Paste the contact's current public key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            contactPublicKey = newKey
            contactKeyDate = dateFormat.format(Date())
            etNewKey.text.clear()
            refreshLockState()
            Toast.makeText(this, "Chat unlocked", Toast.LENGTH_SHORT).show()
        }

        btnSendChat.setOnClickListener {
            val text = etChatInput.text.toString().trim()
            val key = contactPublicKey
            if (text.isEmpty() || key.isNullOrEmpty()) return@setOnClickListener

            lifecycleScope.launch {
                val myId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@launch
                try {
                    val payload = EncryptionHelper.encrypt(text, key)
                    SupabaseClientProvider.client.from("messages").insert(
                        MessageInsert(
                            sender_id = myId,
                            recipient_id = contactId,
                            encrypted_aes_key = payload.encryptedAesKey,
                            iv = payload.iv,
                            ciphertext = payload.ciphertext
                        )
                    )

                    messages.add(
                        ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            text = text,
                            isSentByMe = true,
                            timestamp = timeFormat.format(Date())
                        )
                    )
                    adapter.notifyItemInserted(messages.size - 1)
                    rvChat.scrollToPosition(messages.size - 1)
                    etChatInput.text.clear()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ChatActivity, "Couldn't send. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadContactKey()
        loadThread()
    }

    override fun onResume() {
        super.onResume()
        // Picks up a key the contact may have sent (via Key Requests) while
        // this screen wasn't in front — unlocks the chat automatically
        // without the user needing to leave and come back manually.
        loadContactKey()
    }

    private fun refreshLockState() {
        val today = dateFormat.format(Date())
        val isUnlocked = contactKeyDate == today && !contactPublicKey.isNullOrEmpty()

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

    private fun loadContactKey() {
        lifecycleScope.launch {
            try {
                val record = SupabaseClientProvider.client.from("public_keys")
                    .select { filter { eq("user_id", contactId) } }
                    .decodeSingleOrNull<PublicKeyRecord>()

                val wasLocked = contactKeyDate != dateFormat.format(Date())
                contactPublicKey = record?.public_key
                contactKeyDate = record?.key_date

                refreshLockState()

                // Only announce it if the screen was actually locked before this check.
                val isNowUnlocked = contactKeyDate == dateFormat.format(Date()) && !contactPublicKey.isNullOrEmpty()
                if (wasLocked && isNowUnlocked) {
                    Toast.makeText(this@ChatActivity, "$contactName's key is now up to date", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadThread() {
        lifecycleScope.launch {
            val myId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@launch
            try {
                val received = SupabaseClientProvider.client.from("messages")
                    .select {
                        filter {
                            eq("sender_id", contactId)
                            eq("recipient_id", myId)
                        }
                    }
                    .decodeList<MessageRow>()

                val decryptedReceived = received.mapNotNull { row ->
                    val text = EncryptionHelper.decrypt(
                        EncryptedPayload(row.encrypted_aes_key, row.iv, row.ciphertext)
                    ) ?: return@mapNotNull null
                    ChatMessage(
                        id = row.id,
                        text = text,
                        isSentByMe = false,
                        timestamp = formatTimestamp(row.created_at)
                    )
                }

                messages.clear()
                messages.addAll(decryptedReceived)
                messages.sortBy { it.id }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) rvChat.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatTimestamp(isoTimestamp: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoTimestamp.substring(0, 19)) ?: return ""
            timeFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        const val EXTRA_CONTACT_ID = "extra_contact_id"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
    }
}