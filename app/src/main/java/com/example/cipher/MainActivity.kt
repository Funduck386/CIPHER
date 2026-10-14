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
import androidx.lifecycle.lifecycleScope
import com.example.cipher.crypto.KeyManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Rotates in today's keypair and clears out yesterday's private key
        KeyManager.ensureTodayKeyPair()

        // The rotation above only changes what's stored locally. If the key
        // actually changed (new day, fresh install, etc.) without the user
        // going through LoginActivity again, Supabase's public_keys table
        // would otherwise keep serving the *old* key to anyone who looks this
        // user up — making anything newly sent to them undecryptable. So we
        // re-sync on every MainActivity open too, not just right after login.
        syncPublicKeyIfNeeded()

        val tvPublicKey = findViewById<TextView>(R.id.tvPublicKey)
        val btnCopyKey = findViewById<LinearLayout>(R.id.btnCopyKey)
        val btnShareKey = findViewById<LinearLayout>(R.id.btnShareKey)
        val btnSendMessage = findViewById<LinearLayout>(R.id.btnSendMessage)
        val btnInbox = findViewById<LinearLayout>(R.id.btnInbox)
        val btnKeyRequests = findViewById<LinearLayout>(R.id.btnKeyRequests)

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
            startActivity(Intent(this, InboxActivity::class.java))
        }

        btnKeyRequests.setOnClickListener {
            startActivity(Intent(this, KeyRequestsActivity::class.java))
        }
    }

    private fun syncPublicKeyIfNeeded() {
        val publicKeyBase64 = KeyManager.getPublicKeyBase64() ?: return
        val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return

        lifecycleScope.launch {
            val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            try {
                SupabaseClientProvider.client.from("public_keys").upsert(
                    PublicKeyRecord(
                        user_id = userId,
                        public_key = publicKeyBase64,
                        key_date = today
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Couldn't sync your public key. Others may not be able to message you until this succeeds — try reopening the app on a better connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}