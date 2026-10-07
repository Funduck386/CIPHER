package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cipher.crypto.KeyManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

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
        // TODO: upload publicKeyBase64 to Supabase (Postgrest), tied to this
        // user's account id and today's date, so others can fetch it

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}