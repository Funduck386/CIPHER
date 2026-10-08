package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cipher.crypto.KeyManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnGoogle = findViewById<Button>(R.id.btnGoogleSignIn)
        val btnGithub = findViewById<Button>(R.id.btnGithubSignIn)
        val btnGuest = findViewById<Button>(R.id.tvGuestMode)

        // Watches Supabase's auth state. OAuth sign-in finishes asynchronously
        // (the user leaves the app to a browser and comes back), so this is
        // what actually triggers onAuthSuccess() once a session exists.
        lifecycleScope.launch {
            SupabaseClientProvider.client.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    onAuthSuccess()
                }
            }
        }

        btnGoogle.setOnClickListener {
            lifecycleScope.launch {
                SupabaseClientProvider.client.auth.signInWith(Google)
            }
        }

        btnGithub.setOnClickListener {
            lifecycleScope.launch {
                SupabaseClientProvider.client.auth.signInWith(Github)
            }
        }

        btnGuest.setOnClickListener {
            lifecycleScope.launch {
                SupabaseClientProvider.client.auth.signInAnonymously()
            }
        }
    }

    private fun onAuthSuccess() {
        // Generates today's keypair if this device doesn't have one yet;
        // MainActivity also calls this on every launch to keep the key current.
        KeyManager.ensureTodayKeyPair()

        val publicKeyBase64 = KeyManager.getPublicKeyBase64()
        val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id

        if (publicKeyBase64 != null && userId != null) {
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
                        this@LoginActivity,
                        "Couldn't sync your public key. You can keep using the app, " +
                                "but others may not be able to message you until this succeeds.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                goToMain()
            }
        } else {
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}