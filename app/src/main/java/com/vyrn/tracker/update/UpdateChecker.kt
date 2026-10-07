package com.vyrn.tracker.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Controlla se su GitHub Releases c'è una versione più recente. L'unica chiamata di rete dell'app, disattivabile dalle impostazioni. */
object UpdateChecker {
    private const val API = "https://api.github.com/repos/xX-VyrnCore-Xx/Vyrn-Tracker/releases/latest"
    private const val PREFS = "vyrn_prefs"

    data class Info(val version: String, val url: String)

    fun enabled(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("check_updates", true)

    fun setEnabled(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("check_updates", value).apply()
    }

    fun dismissedVersion(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("update_dismissed", "").orEmpty()

    fun dismiss(ctx: Context, version: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("update_dismissed", version).apply()
    }

    /** true se [candidate] (es. "1.0.12") è più nuova di [current] (es. "1.0.10"). */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    @Suppress("DEPRECATION")
    suspend fun check(ctx: Context): Info? = withContext(Dispatchers.IO) {
        try {
            val current = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: return@withContext null
            val conn = URL(API).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (conn.responseCode != 200) return@withContext null
                val obj = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val tag = obj.getString("tag_name").removePrefix("v")
                val url = obj.getString("html_url")
                if (isNewer(tag, current)) Info(tag, url) else null
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }
}
