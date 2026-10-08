package com.example.cipher

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * One shared Supabase client for the whole app.
 * Fill in SUPABASE_URL and SUPABASE_ANON_KEY from your project's
 * Settings -> API page before running the app.
 */
object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://psrotmrfhkimlsdooqfz.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_ML7njpOKBpSAJ0YKtv6Exw_TVGLE..."

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Must match the scheme/host declared in AndroidManifest.xml's
            // deep-link intent filter on LoginActivity.
            scheme = "cipher"
            host = "login-callback"
        }
        install(Postgrest)
    }
}