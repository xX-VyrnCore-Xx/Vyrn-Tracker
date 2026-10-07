package com.vyrn.tracker.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.ui.theme.AppPalette
import com.vyrn.tracker.ui.theme.Palettes
import com.vyrn.tracker.ui.theme.ThemeSettings

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aspetto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fatto") } },
    )
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
