package com.example.cipher

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cipher.crypto.KeyManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
private data class KeyRequestRow(
    val id: String,
    val from_user_id: String,
    val to_user_id: String,
    val created_at: String
)

class KeyRequestsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_requests)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)
        val rvRequests = findViewById<RecyclerView>(R.id.rvRequests)

        btnBack.setOnClickListener { finish() }
        rvRequests.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val myId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            if (myId == null) {
                tvEmptyState.visibility = View.VISIBLE
                rvRequests.visibility = View.GONE
                return@launch
            }

            try {
                val rows = SupabaseClientProvider.client.from("key_requests")
                    .select { filter { eq("to_user_id", myId) } }
                    .decodeList<KeyRequestRow>()

                val requests = rows.map { row ->
                    KeyRequest(
                        id = row.id,
                        fromContactId = row.from_user_id,
                        fromDisplayName = row.from_user_id.take(8) + "...",
                        requestedAt = formatTimestamp(row.created_at)
                    )
                }.toMutableList()

                fun render() {
                    if (requests.isEmpty()) {
                        tvEmptyState.visibility = View.VISIBLE
                        rvRequests.visibility = View.GONE
                    } else {
                        tvEmptyState.visibility = View.GONE
                        rvRequests.visibility = View.VISIBLE
                    }
                }
                render()

                rvRequests.adapter = KeyRequestAdapter(requests) { request ->
                    lifecycleScope.launch {
                        val myPublicKey = KeyManager.getPublicKeyBase64()
                        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

                        if (myPublicKey == null) {
                            Toast.makeText(this@KeyRequestsActivity, "No local key found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        try {
                            // Make sure the requester can see our current key
                            // (our own row in public_keys should already be current,
                            // but this keeps it correct even if it wasn't synced yet).
                            SupabaseClientProvider.client.from("public_keys").upsert(
                                PublicKeyRecord(
                                    user_id = myId,
                                    public_key = myPublicKey,
                                    key_date = today
                                )
                            )

                            // Mark the request fulfilled by deleting it.
                            SupabaseClientProvider.client.from("key_requests")
                                .delete { filter { eq("id", request.id) } }

                            requests.remove(request)
                            rvRequests.adapter?.notifyDataSetChanged()
                            render()

                            Toast.makeText(this@KeyRequestsActivity, "Key sent to ${request.fromDisplayName}", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@KeyRequestsActivity, "Couldn't send your key. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@KeyRequestsActivity, "Couldn't load requests.", Toast.LENGTH_SHORT).show()
                tvEmptyState.visibility = View.VISIBLE
                rvRequests.visibility = View.GONE
            }
        }
    }

    private fun formatTimestamp(isoTimestamp: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoTimestamp.substring(0, 19)) ?: return "Recently"
            SimpleDateFormat("MMM d, h:mm a", Locale.US).format(date)
        } catch (e: Exception) {
            "Recently"
        }
    }
}