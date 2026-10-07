package com.vyrn.tracker.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.data.Backup
import com.vyrn.tracker.lock.AppLock
import com.vyrn.tracker.ui.theme.AppPalette
import com.vyrn.tracker.ui.theme.Palettes
import com.vyrn.tracker.ui.theme.ThemeSettings
import com.vyrn.tracker.update.UpdateChecker
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var bio by remember { mutableStateOf(AppLock.biometricEnabled(ctx)) }
    var updates by remember { mutableStateOf(UpdateChecker.enabled(ctx)) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = Backup.exportTo(ctx, uri)
            Toast.makeText(ctx, if (ok) "Backup salvato" else "Errore durante il backup", Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Impostazioni") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Palette colori", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Palettes.forEach { p ->
                        PaletteSwatch(p, selected = ThemeSettings.paletteKey == p.key) {
                            ThemeSettings.setPalette(ctx, p.key)
                        }
                    }
                }
                Text(
                    Palettes.firstOrNull { it.key == ThemeSettings.paletteKey }?.label.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Tema", style = MaterialTheme.typography.labelLarge)
                ChipRow(listOf(0, 1, 2), ThemeSettings.mode, { listOf("Sistema", "Chiaro", "Scuro")[it] }) {
                    ThemeSettings.setMode(ctx, it)
                }

                Text("Sicurezza", style = MaterialTheme.typography.labelLarge)
                if (!AppLock.pinSet) {
                    OutlinedButton(onClick = { pinDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Imposta un PIN")
                    }
                } else {
                    val canBio = remember { biometricAvailable(ctx) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sblocco con impronta o volto", Modifier.weight(1f))
                        Switch(
                            checked = bio && canBio,
                            enabled = canBio,
                            onCheckedChange = { bio = it; AppLock.setBiometric(ctx, it) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pinDialog = true }, modifier = Modifier.weight(1f)) { Text("Cambia PIN") }
                        OutlinedButton(onClick = { AppLock.clear(ctx); bio = false }, modifier = Modifier.weight(1f)) {
                            Text("Rimuovi PIN")
                        }
                    }
                }

                Text("Aggiornamenti", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Avvisa se esce una nuova versione")
                        Text(
                            "Usa internet solo per questo controllo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = updates, onCheckedChange = { updates = it; UpdateChecker.setEnabled(ctx, it) })
                }

                Text("Backup completo", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Salva tutti i dati in un file JSON (anche su Google Drive dal selettore file) e ripristinali su un altro telefono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("vyrn-backup-${LocalDate.now()}.json") },
                        modifier = Modifier.weight(1f),
                    ) { Text("Esporta") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Ripristina") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fatto") } },
    )

    if (pinDialog) PinSetupDialog(onDismiss = { pinDialog = false })

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Ripristinare il backup?") },
            text = { Text("I dati attuali verranno sostituiti da quelli del file. L'operazione non si può annullare.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val ok = Backup.importFrom(ctx, uri)
                        Toast.makeText(
                            ctx,
                            if (ok) "Backup ripristinato" else "File non valido o errore di lettura",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }) { Text("Ripristina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val len = AppLock.PIN_LENGTH
    FormDialog(
        title = "Imposta PIN",
        onDismiss = onDismiss,
        confirmEnabled = first.length == len && first == second,
        onConfirm = {
            AppLock.setPin(ctx, first)
            onDismiss()
        },
    ) {
        OutlinedTextField(
            value = first,
            onValueChange = { if (it.length <= len && it.all { c -> c.isDigit() }) first = it },
            label = { Text("Nuovo PIN ($len cifre)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = second,
            onValueChange = { if (it.length <= len && it.all { c -> c.isDigit() }) second = it },
            label = { Text("Ripeti il PIN") },
            singleLine = true,
            isError = second.isNotEmpty() && first != second,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PaletteSwatch(p: AppPalette, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swatch",
    )
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        label = "ring",
    )
    Box(
        Modifier.size(34.dp).scale(scale).clip(CircleShape)
            .background(Brush.linearGradient(listOf(p.light, p.accent)))
            .border(2.dp, ring, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
    }
}
