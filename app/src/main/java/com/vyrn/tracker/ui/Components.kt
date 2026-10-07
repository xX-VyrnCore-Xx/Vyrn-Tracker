@file:OptIn(ExperimentalMaterial3Api::class)

package com.vyrn.tracker.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.vyrn.tracker.data.WEEKDAY_LETTER
import com.vyrn.tracker.data.fmtDay
import com.vyrn.tracker.data.fmtTime
import java.time.LocalDate

val ItemColors: List<Color> = listOf(
    0xFF7C5CFF, 0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFFEC4899, 0xFF14B8A6, 0xFF8B5CF6,
).map { Color(it) }

fun colorOf(idx: Int): Color = ItemColors[idx.mod(ItemColors.size)]

val HabitEmojis = listOf("✅", "💧", "🏃", "📚", "🧘", "💪", "🛌", "🥗", "🚭", "🦷", "✍️", "🎸", "🧹", "💊", "🌳", "📵")
val RoutineEmojis = listOf("🌅", "🌞", "🌙", "🧼", "☕", "🏋️", "📖", "🧺")
val AccountEmojis = listOf("💵", "💳", "🏦", "🐷", "📱", "💼", "🪙", "🏠")
val CategoryEmojis = listOf("🛒", "🍽️", "🚗", "🏠", "💡", "🎉", "👕", "💊", "📚", "✈️", "📦", "💼", "🎁", "📈", "💰", "🐶")
val GoalEmojis = listOf("🎯", "✈️", "🚗", "🏠", "💻", "📱", "🎓", "💍", "🛟", "🎁")

@Composable
fun ScreenScaffold(title: String, actions: @Composable RowScope.() -> Unit = {}, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                actions = actions,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) { content() }
    }
}

@Composable
fun VCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
fun EmptyState(emoji: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmojiBadge(emoji: String, color: Color, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, style = MaterialTheme.typography.titleMedium) }
}

@Composable
fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "Salva",
    confirmEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var askDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) } },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { askDelete = true }) {
                        Text("Elimina", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        },
    )
    if (askDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Eliminare?") },
            text = { Text("L'operazione non può essere annullata.") },
            confirmButton = {
                TextButton(onClick = { askDelete = false; onDelete() }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Annulla") } },
        )
    }
}

@Composable
fun EmojiPicker(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { e ->
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(
                        if (e == selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) { Text(e) }
        }
    }
}

@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ItemColors.indices.toList()) { i ->
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(ItemColors[i]).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { if (i == selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun WeekdayChips(mask: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        WEEKDAY_LETTER.forEachIndexed { i, letter ->
            val on = (mask shr i) and 1 == 1
            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onChange(mask xor (1 shl i)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o), maxLines = 1) })
        }
    }
}

@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (selected == null) label else "$label: ${text(selected)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

@Composable
fun DatePickerField(label: String, day: Long?, onChange: (Long?) -> Unit, clearable: Boolean = false) {
    var show by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { show = true }, modifier = Modifier.weight(1f)) {
            Text(if (day == null) label else "$label: ${fmtDay(day)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (clearable && day != null) {
            IconButton(onClick = { onChange(null) }) { Icon(Icons.Rounded.Close, contentDescription = "Rimuovi") }
        }
    }
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (day ?: LocalDate.now().toEpochDay()) * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Math.floorDiv(it, 86_400_000L)) }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Annulla") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
fun TimeField(label: String, minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    OutlinedButton(
        modifier = modifier,
        onClick = {
            val h = if (minutes >= 0) minutes / 60 else 8
            val m = if (minutes >= 0) minutes % 60 else 0
            TimePickerDialog(ctx, { _, hh, mm -> onChange(hh * 60 + mm) }, h, m, true).show()
        },
    ) { Text("$label: ${fmtTime(minutes)}") }
}

@Composable
fun ReminderField(minutes: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Promemoria", Modifier.weight(1f))
        if (minutes >= 0) {
            TimeField("Ora", minutes, onChange)
            Spacer(Modifier.size(8.dp))
        }
        Switch(checked = minutes >= 0, onCheckedChange = { onChange(if (it) 8 * 60 else -1) })
    }
}

@Composable
fun DecimalField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.all { it.isDigit() || it == ',' || it == '.' || it == '-' }) onChange(v) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun TextInput(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
    )
}

val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
