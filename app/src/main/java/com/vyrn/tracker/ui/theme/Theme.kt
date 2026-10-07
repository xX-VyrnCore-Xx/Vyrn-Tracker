package com.vyrn.tracker.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val IncomeColor = Color(0xFF2EB67D)
val ExpenseColor = Color(0xFFE5484D)
val WarnColor = Color(0xFFF5A524)

/** Palette selezionabile dall'utente. [light] e [dark] sono il colore primario nei due temi, [accent] chiude il gradiente. */
data class AppPalette(val key: String, val label: String, val light: Color, val dark: Color, val accent: Color)

val Palettes = listOf(
    AppPalette("viola", "Viola", Color(0xFF5B3FD6), Color(0xFFB9A8FF), Color(0xFFA06BFF)),
    AppPalette("smeraldo", "Smeraldo", Color(0xFF0B8A64), Color(0xFF6EE7B7), Color(0xFF14C8A8)),
    AppPalette("oceano", "Oceano", Color(0xFF1D63D8), Color(0xFF9CC4FF), Color(0xFF2CB8F0)),
    AppPalette("tramonto", "Tramonto", Color(0xFFD9480F), Color(0xFFFFB38A), Color(0xFFF5A524)),
    AppPalette("rosa", "Rosa", Color(0xFFC2185B), Color(0xFFFFA6C9), Color(0xFFF0509A)),
    AppPalette("grafite", "Grafite", Color(0xFF3B4252), Color(0xFFB8C2D6), Color(0xFF6B7A99)),
)

fun paletteByKey(key: String): AppPalette = Palettes.firstOrNull { it.key == key } ?: Palettes.first()

/** Impostazioni d'aspetto persistite in SharedPreferences. mode: 0 = sistema, 1 = chiaro, 2 = scuro. */
object ThemeSettings {
    private const val PREFS = "vyrn_prefs"

    var paletteKey by mutableStateOf("viola")
        private set
    var mode by mutableIntStateOf(0)
        private set

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        paletteKey = p.getString("palette", "viola") ?: "viola"
        mode = p.getInt("mode", 0)
    }

    fun setPalette(context: Context, key: String) {
        paletteKey = key
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("palette", key).apply()
    }

    fun setMode(context: Context, value: Int) {
        mode = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("mode", value).apply()
    }
}

/** Gradiente usato nelle card "hero". */
val LocalGradient = staticCompositionLocalOf { listOf(Color(0xFF5B3FD6), Color(0xFFA06BFF)) }

private fun schemeFor(p: AppPalette, dark: Boolean): ColorScheme {
    return if (dark) {
        val pr = p.dark
        val bg = lerp(Color(0xFF0E0D12), pr, 0.05f)
        darkColorScheme(
            primary = pr,
            onPrimary = lerp(pr, Color.Black, 0.78f),
            primaryContainer = lerp(pr, Color.Black, 0.62f),
            onPrimaryContainer = lerp(pr, Color.White, 0.75f),
            secondary = lerp(pr, Color(0xFFCFCFD8), 0.55f),
            onSecondary = lerp(pr, Color.Black, 0.8f),
            secondaryContainer = lerp(bg, pr, 0.22f),
            onSecondaryContainer = lerp(pr, Color.White, 0.8f),
            tertiary = p.accent,
            background = bg,
            onBackground = Color(0xFFE8E4EE),
            surface = bg,
            onSurface = Color(0xFFE8E4EE),
            surfaceVariant = lerp(bg, pr, 0.16f),
            onSurfaceVariant = Color(0xFFCBC6D2),
            surfaceContainer = lerp(bg, pr, 0.09f),
            surfaceContainerHigh = lerp(bg, pr, 0.15f),
            surfaceContainerHighest = lerp(bg, pr, 0.21f),
            outline = Color(0xFF948E9D),
            outlineVariant = lerp(bg, pr, 0.30f),
        )
    } else {
        val pr = p.light
        val bg = lerp(Color.White, pr, 0.035f)
        lightColorScheme(
            primary = pr,
            onPrimary = Color.White,
            primaryContainer = lerp(pr, Color.White, 0.85f),
            onPrimaryContainer = lerp(pr, Color.Black, 0.65f),
            secondary = lerp(pr, Color(0xFF605A70), 0.6f),
            onSecondary = Color.White,
            secondaryContainer = lerp(Color.White, pr, 0.16f),
            onSecondaryContainer = lerp(pr, Color.Black, 0.7f),
            tertiary = p.accent,
            background = bg,
            onBackground = Color(0xFF1C1B20),
            surface = bg,
            onSurface = Color(0xFF1C1B20),
            surfaceVariant = lerp(Color.White, pr, 0.14f),
            onSurfaceVariant = Color(0xFF4A4650),
            surfaceContainer = lerp(Color.White, pr, 0.08f),
            surfaceContainerHigh = lerp(Color.White, pr, 0.12f),
            surfaceContainerHighest = lerp(Color.White, pr, 0.16f),
            outline = Color(0xFF7A7483),
            outlineVariant = lerp(Color.White, pr, 0.25f),
        )
    }
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun VyrnTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeSettings.mode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val palette = paletteByKey(ThemeSettings.paletteKey)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalGradient provides listOf(palette.light, palette.accent)) {
        MaterialTheme(
            colorScheme = schemeFor(palette, dark),
            shapes = AppShapes,
            content = content,
        )
    }
}
