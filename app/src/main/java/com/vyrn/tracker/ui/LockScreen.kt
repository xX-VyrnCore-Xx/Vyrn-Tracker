package com.vyrn.tracker.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vyrn.tracker.lock.AppLock
import kotlinx.coroutines.delay

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

fun biometricAvailable(ctx: Context): Boolean =
    BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS

private fun showBiometricPrompt(ctx: Context, onSuccess: () -> Unit) {
    val activity = ctx.findFragmentActivity() ?: return
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Sblocca Vyrn Tracker")
        .setNegativeButtonText("Usa il PIN")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}

@Composable
fun LockScreen() {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var remaining by remember { mutableLongStateOf(AppLock.lockoutRemainingMs(ctx)) }
    val useBio = remember { AppLock.biometricEnabled(ctx) && biometricAvailable(ctx) }

    val unlockNow = {
        AppLock.registerSuccess(ctx)
        AppLock.unlock()
    }

    BackHandler { }
    LaunchedEffect(Unit) {
        if (useBio) showBiometricPrompt(ctx) { unlockNow() }
    }
    LaunchedEffect(remaining > 0) {
        while (remaining > 0) {
            delay(500)
            remaining = AppLock.lockoutRemainingMs(ctx)
        }
    }

    fun press(d: String) {
        if (remaining > 0 || pin.length >= AppLock.PIN_LENGTH) return
        error = false
        pin += d
        if (pin.length == AppLock.PIN_LENGTH) {
            if (AppLock.verify(ctx, pin)) unlockNow()
            else {
                AppLock.registerFailure(ctx)
                remaining = AppLock.lockoutRemainingMs(ctx)
                error = true
                pin = ""
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🔒", style = MaterialTheme.typography.displayMedium)
            Text("Vyrn Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                when {
                    remaining > 0 -> "Troppi tentativi. Riprova tra ${(remaining + 999) / 1000} s"
                    error -> "PIN errato, riprova"
                    else -> "Inserisci il PIN"
                },
                color = if (error || remaining > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(AppLock.PIN_LENGTH) { i ->
                    Box(
                        Modifier.size(14.dp).clip(CircleShape).background(
                            if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
            rows.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    r.forEach { d -> PadKey(d) { press(d) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                if (useBio) {
                    PadIcon(onClick = { showBiometricPrompt(ctx) { unlockNow() } }) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = "Impronta")
                    }
                } else Spacer(Modifier.size(68.dp))
                PadKey("0") { press("0") }
                PadIcon(onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Cancella")
                }
            }
        }
    }
}

@Composable
private fun PadKey(label: String, onClick: () -> Unit) {
    PadIcon(onClick) { Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium) }
}

@Composable
private fun PadIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
