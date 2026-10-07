package com.vyrn.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9A8FF),
    onPrimary = Color(0xFF2A1A6B),
    primaryContainer = Color(0xFF41308A),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFCBC2E8),
    onSecondary = Color(0xFF332D4C),
    secondaryContainer = Color(0xFF3A3453),
    onSecondaryContainer = Color(0xFFE8DFFF),
    tertiary = Color(0xFF4FD1A5),
    background = Color(0xFF121018),
    onBackground = Color(0xFFE7E1EC),
    surface = Color(0xFF121018),
    onSurface = Color(0xFFE7E1EC),
    surfaceVariant = Color(0xFF2A2534),
    onSurfaceVariant = Color(0xFFCBC4D4),
    surfaceContainer = Color(0xFF1E1A27),
    surfaceContainerHigh = Color(0xFF2A2534),
    surfaceContainerHighest = Color(0xFF342F3F),
    outline = Color(0xFF948E9D),
    outlineVariant = Color(0xFF494453),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B3FD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1B0063),
    secondary = Color(0xFF605A7D),
    secondaryContainer = Color(0xFFE6DFFF),
    onSecondaryContainer = Color(0xFF1D1736),
    tertiary = Color(0xFF0E8F6A),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E0F0),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainer = Color(0xFFF1EBF8),
    surfaceContainerHigh = Color(0xFFEAE4F2),
    surfaceContainerHighest = Color(0xFFE4DEEC),
    outline = Color(0xFF7A7483),
    outlineVariant = Color(0xFFCBC4D4),
)

val IncomeColor = Color(0xFF2EB67D)
val ExpenseColor = Color(0xFFE5484D)
val WarnColor = Color(0xFFF5A524)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun VyrnTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
