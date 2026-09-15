package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.cipher.crypto.KeyManager

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmailOrPhone = findViewById<EditText>(R.id.etEmailOrPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleSignIn)
        val btnApple = findViewById<Button>(R.id.btnAppleSignIn)
        val btnPhone = findViewById<Button>(R.id.btnPhoneSignIn)
        val btnGuest = findViewById<Button>(R.id.tvGuestMode)

        btnLogin.setOnClickListener {
            val identifier = etEmailOrPhone.text.toString()
            val password = etPassword.text.toString()
            // TODO: call Supabase email/password sign-in with identifier + password
            onAuthSuccess()
        }

        btnGoogle.setOnClickListener {
            // TODO: wire up Supabase Google OAuth sign-in
            onAuthSuccess()
        }

        btnApple.setOnClickListener {
            // TODO: wire up Supabase Apple OAuth sign-in
            onAuthSuccess()
        }

        btnPhone.setOnClickListener {
            // TODO: navigate to phone number entry + OTP screen
        }

        btnGuest.setOnClickListener {
            // TODO: create anonymous Supabase session
            onAuthSuccess()
        }
    }

    /**
     * Called after any successful sign-in (new or returning user).
     * If this device has no keypair yet, generate one and upload the public key.
     */
    private fun onAuthSuccess() {
        if (!KeyManager.hasKeyPair()) {
            KeyManager.generateKeyPairIfNeeded()
            val publicKeyBase64 = KeyManager.getPublicKeyBase64()
            // TODO: upload publicKeyBase64 to Supabase, tied to this user's account
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}