package com.example.cipher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnGoogle = findViewById<Button>(R.id.btnGoogleSignIn)
        val btnApple = findViewById<Button>(R.id.btnAppleSignIn)
        val btnPhone = findViewById<Button>(R.id.btnPhoneSignIn)
        val tvGuest = findViewById<TextView>(R.id.tvGuestMode)

        btnGoogle.setOnClickListener {
            // TODO: wire up Supabase Google OAuth sign-in
            signInSuccess()
        }

        btnApple.setOnClickListener {
            // TODO: wire up Supabase Apple OAuth sign-in
            signInSuccess()
        }

        btnPhone.setOnClickListener {
            // TODO: navigate to phone number entry + OTP screen
        }

        tvGuest.setOnClickListener {
            // TODO: create anonymous Supabase session
            signInSuccess()
        }
    }

    private fun signInSuccess() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}