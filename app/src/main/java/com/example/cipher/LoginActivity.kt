package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cipher.crypto.KeyManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    // Supabase's sessionStatus can emit Authenticated more than once (e.g. once
    // from the stored session, once more after a deep link refreshes it). This
    // guard stops onAuthSuccess() — and the navigation to MainActivity it
    // triggers — from running more than once per app open.
    private var hasHandledAuthSuccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // If this activity was (re)launched by the OAuth browser redirect
        // (cipher://login-callback), hand that intent to Supabase so it can
        // read the access token out of it and turn it into a real session.
        intent?.let { SupabaseClientProvider.client.handleDeeplinks(it) }

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
                try {
                    SupabaseClientProvider.client.auth.signInWith(Google)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@LoginActivity,
                        "Couldn't reach the server. Check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        btnGithub.setOnClickListener {
            lifecycleScope.launch {
                try {
                    SupabaseClientProvider.client.auth.signInWith(Github)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@LoginActivity,
                        "Couldn't reach the server. Check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        btnGuest.setOnClickListener {
            lifecycleScope.launch {
                try {
                    SupabaseClientProvider.client.auth.signInAnonymously()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@LoginActivity,
                        "Couldn't reach the server. Check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Covers the case where Android reuses the existing LoginActivity
        // instance instead of creating a new one when the browser redirects
        // back (this is why LoginActivity is launchMode="singleTask" in the
        // manifest) — onCreate won't run again, so we handle the deep link here.
        setIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent)
    }

    private fun onAuthSuccess() {
        if (hasHandledAuthSuccess) return
        hasHandledAuthSuccess = true

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
                        "Couldn't sync your public key. You can keep using the app, but others may not be able to message you until this succeeds.",
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