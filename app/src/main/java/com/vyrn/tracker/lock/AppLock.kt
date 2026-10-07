package com.vyrn.tracker.lock

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import java.security.SecureRandom

/** Blocco dell'app con PIN a 4 cifre (hash con sale in SharedPreferences) e sblocco biometrico opzionale. */
object AppLock {
    private const val PREFS = "vyrn_lock"
    private const val RELOCK_AFTER_MS = 15_000L

    const val PIN_LENGTH = 4

    var locked by mutableStateOf(false)
        private set
    var pinSet by mutableStateOf(false)
        private set

    private var backgroundedAt = 0L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var initialized = false

    /** Da chiamare all'avvio: un nuovo processo parte sempre bloccato, una rotazione no. */
    fun init(ctx: Context) {
        pinSet = prefs(ctx).contains("hash")
        if (!initialized) {
            initialized = true
            locked = pinSet
        }
    }

    fun biometricEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean("bio", false)

    fun setBiometric(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean("bio", enabled).apply()
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun setPin(ctx: Context, pin: String) {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = saltBytes.joinToString("") { "%02x".format(it) }
        prefs(ctx).edit().putString("salt", salt).putString("hash", hash(salt, pin)).apply()
        pinSet = true
    }

    fun verify(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        val salt = p.getString("salt", null) ?: return false
        return p.getString("hash", null) == hash(salt, pin)
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        pinSet = false
        locked = false
    }

    // ---- Protezione dai tentativi ripetuti: dopo 5 errori attesa crescente (30 s, 60 s, ... max 15 min) ----
    private const val MAX_ATTEMPTS = 5

    fun lockoutRemainingMs(ctx: Context): Long =
        (prefs(ctx).getLong("until", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun registerFailure(ctx: Context) {
        val p = prefs(ctx)
        val fails = p.getInt("fails", 0) + 1
        val e = p.edit().putInt("fails", fails)
        if (fails >= MAX_ATTEMPTS) {
            val step = (fails - MAX_ATTEMPTS).coerceAtMost(5)
            val delay = minOf(30_000L shl step, 15 * 60_000L)
            e.putLong("until", System.currentTimeMillis() + delay)
        }
        e.apply()
    }

    fun registerSuccess(ctx: Context) {
        prefs(ctx).edit().putInt("fails", 0).putLong("until", 0L).apply()
    }

    fun unlock() {
        locked = false
    }

    fun onBackground() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        if (pinSet && SystemClock.elapsedRealtime() - backgroundedAt > RELOCK_AFTER_MS) locked = true
    }
}
