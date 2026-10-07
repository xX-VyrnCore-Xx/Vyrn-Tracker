package com.vyrn.tracker.ui.routine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.RoutineViewModel
import com.vyrn.tracker.data.Routine
import com.vyrn.tracker.data.RoutineStep
import com.vyrn.tracker.data.isWeekdayOn
import com.vyrn.tracker.data.weekdaysText
import com.vyrn.tracker.ui.ChipRow
import com.vyrn.tracker.ui.ColorPicker
import com.vyrn.tracker.ui.EmojiBadge
import com.vyrn.tracker.ui.EmojiPicker
import com.vyrn.tracker.ui.EmptyState
import com.vyrn.tracker.ui.FormDialog
import com.vyrn.tracker.ui.ReminderField
import com.vyrn.tracker.ui.RoutineEmojis
import com.vyrn.tracker.ui.ScreenPadding
import com.vyrn.tracker.ui.TextInput
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.WeekdayChips
import com.vyrn.tracker.ui.colorOf
import java.time.LocalDate

private val PERIODS = listOf("Mattina", "Pomeriggio", "Sera")

private data class StepDraft(val id: Long, val title: String, val minutes: String)

@Composable
fun RoutinesTab(vm: RoutineViewModel) {
    val routines by vm.routines.collectAsState()
    val steps by vm.steps.collectAsState()
    val stepLogs by vm.stepLogs.collectAsState()
    val todayDate = LocalDate.now()
    val today = todayDate.toEpochDay()
    val doneToday = remember(stepLogs, today) { stepLogs.filter { it.day == today }.map { it.stepId }.toSet() }
    var editor by remember { mutableStateOf<Routine?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (routines.isEmpty()) {
                item { EmptyState("🌅", "Nessuna routine.\nCrea una sequenza di passi per la mattina o la sera.") }
            }
            items(routines, key = { it.id }) { r ->
                val rSteps = steps.filter { it.routineId == r.id }
                val done = rSteps.count { it.id in doneToday }
                val color = colorOf(r.colorIdx)
                val scheduled = isWeekdayOn(r.weekdaysMask, todayDate)
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(r.icon, color)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${PERIODS[r.period.coerceIn(0, 2)]} · ${weekdaysText(r.weekdaysMask)}" + if (scheduled) "" else " · non prevista oggi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editor = r }) { Icon(Icons.Rounded.Edit, contentDescription = "Modifica") }
                    }
                    if (rSteps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { done.toFloat() / rSteps.size },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = color,
                        )
                        Text(
                            "$done/${rSteps.size} completati oggi",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    rSteps.forEach { s ->
                        val isDone = s.id in doneToday
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.toggleStep(s.id, today, isDone) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = isDone, onCheckedChange = { vm.toggleStep(s.id, today, isDone) })
                            Text(
                                s.title,
                                modifier = Modifier.weight(1f),
                                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            if (s.durationMin > 0) {
                                Text("${s.durationMin} min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editor = Routine(name = "") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Nuova routine") }
    }

    editor?.let { r ->
        RoutineEditor(
            initial = r,
            initialSteps = steps.filter { it.routineId == r.id },
            onDismiss = { editor = null },
            onSave = { routine, list -> vm.saveRoutine(routine, list); editor = null },
            onDelete = if (r.id != 0L) ({ vm.deleteRoutine(r); editor = null }) else null,
        )
    }
}

@Composable
private fun RoutineEditor(
    initial: Routine,
    initialSteps: List<RoutineStep>,
    onDismiss: () -> Unit,
    onSave: (Routine, List<RoutineStep>) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var colorIdx by remember { mutableStateOf(initial.colorIdx) }
    var period by remember { mutableStateOf(initial.period) }
    var mask by remember { mutableStateOf(initial.weekdaysMask) }
    var reminder by remember { mutableStateOf(initial.reminderMinutes) }
    val drafts = remember {
        mutableStateListOf<StepDraft>().also { list ->
            initialSteps.forEach { list.add(StepDraft(it.id, it.title, if (it.durationMin > 0) it.durationMin.toString() else "")) }
        }
    }

    FormDialog(
        title = if (initial.id == 0L) "Nuova routine" else "Modifica routine",
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && mask != 0,
        onDelete = onDelete,
        onConfirm = {
            val steps = drafts.filter { it.title.isNotBlank() }.map {
                RoutineStep(id = it.id, routineId = initial.id, title = it.title.trim(), durationMin = it.minutes.toIntOrNull() ?: 0)
            }
            onSave(
                initial.copy(name = name.trim(), icon = icon, colorIdx = colorIdx, period = period, weekdaysMask = mask, reminderMinutes = reminder),
                steps,
            )
        },
    ) {
        TextInput("Nome", name, { name = it })
        EmojiPicker(RoutineEmojis, icon) { icon = it }
        ColorPicker(colorIdx) { colorIdx = it }
        ChipRow(listOf(0, 1, 2), period, { PERIODS[it] }) { period = it }
        Text("Giorni", style = MaterialTheme.typography.labelLarge)
        WeekdayChips(mask) { mask = it }
        ReminderField(reminder) { reminder = it }
        Text("Passi", style = MaterialTheme.typography.labelLarge)
        drafts.forEachIndexed { i, d ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextInput("Passo ${i + 1}", d.title, { drafts[i] = d.copy(title = it) }, Modifier.weight(1f))
                TextInput("Min", d.minutes, { v -> if (v.all { it.isDigit() } && v.length <= 3) drafts[i] = d.copy(minutes = v) }, Modifier.width(72.dp))
                IconButton(onClick = { drafts.removeAt(i) }) { Icon(Icons.Rounded.Close, contentDescription = "Rimuovi passo") }
            }
        }
        TextButton(onClick = { drafts.add(StepDraft(0L, "", "")) }) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Aggiungi passo")
        }
    }
}
