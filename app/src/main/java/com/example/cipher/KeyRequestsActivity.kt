package com.example.cipher

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cipher.crypto.KeyManager

class KeyRequestsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_requests)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)
        val rvRequests = findViewById<RecyclerView>(R.id.rvRequests)

        btnBack.setOnClickListener { finish() }

        // TODO: replace with real pending key_request rows fetched from Supabase for this user
        val requests = mutableListOf<KeyRequest>()

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

        rvRequests.layoutManager = LinearLayoutManager(this)
        rvRequests.adapter = KeyRequestAdapter(requests) { request ->
            val myPublicKey = KeyManager.getPublicKeyBase64()
            // TODO: upload a key_update to Supabase: { from: myUserId, to: request.fromContactId, publicKey: myPublicKey }
            // TODO: delete/mark this key_request as fulfilled
            Toast.makeText(this, "Key sent to ${request.fromDisplayName}", Toast.LENGTH_SHORT).show()
        }
    }
}