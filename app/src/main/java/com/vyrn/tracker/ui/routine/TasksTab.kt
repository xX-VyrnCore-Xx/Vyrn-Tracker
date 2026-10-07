package com.vyrn.tracker.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.DeepLink
import com.vyrn.tracker.RoutineViewModel
import com.vyrn.tracker.data.Task
import com.vyrn.tracker.data.fmtDay
import com.vyrn.tracker.data.fmtTime
import com.vyrn.tracker.ui.ChipRow
import com.vyrn.tracker.ui.DatePickerField
import com.vyrn.tracker.ui.EmptyState
import com.vyrn.tracker.ui.FormDialog
import com.vyrn.tracker.ui.ScreenPadding
import com.vyrn.tracker.ui.TextInput
import com.vyrn.tracker.ui.TimeField
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.theme.ExpenseColor
import com.vyrn.tracker.ui.theme.IncomeColor
import com.vyrn.tracker.ui.theme.WarnColor
import java.time.LocalDate

private val PRIORITIES = listOf("Bassa", "Media", "Alta")

private fun priorityColor(p: Int) = when (p) {
    2 -> ExpenseColor
    1 -> WarnColor
    else -> IncomeColor
}

@Composable
fun TasksTab(vm: RoutineViewModel) {
    val tasks by vm.tasks.collectAsState()
    var showDone by rememberSaveable { mutableStateOf(false) }
    var editor by remember { mutableStateOf<Task?>(null) }
    var quick by remember { mutableStateOf("") }
    LaunchedEffect(DeepLink.action) {
        if (DeepLink.action == DeepLink.NEW_TASK) {
            editor = Task(title = "")
            DeepLink.consume()
        }
    }
    val today = LocalDate.now().toEpochDay()
    val list = tasks.filter { it.done == showDone }
        .sortedWith(compareBy<Task>({ if (it.dueDay < 0) Long.MAX_VALUE else it.dueDay }, { -it.priority }))

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                ChipRow(listOf(false, true), showDone, { if (it) "Completati" else "Da fare" }) { showDone = it }
            }
            item {
                if (!showDone) {
                    val submit = {
                        if (quick.isNotBlank()) {
                            vm.saveTask(Task(title = quick.trim()))
                            quick = ""
                        }
                    }
                    OutlinedTextField(
                        value = quick,
                        onValueChange = { quick = it },
                        label = { Text("Aggiungi un task al volo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailingIcon = {
                            IconButton(onClick = { submit() }) { Icon(Icons.Rounded.Add, contentDescription = "Aggiungi") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        if (showDone) "🗂️" else "🎉",
                        if (showDone) "Nessun task completato." else "Nessun task da fare.\nTocca + per aggiungerne uno.",
                    )
                }
            }
            items(list, key = { it.id }) { t ->
                VCard(modifier = Modifier.animateItem(), onClick = { editor = t }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = t.done, onCheckedChange = { vm.toggleTask(t) })
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (t.done) TextDecoration.LineThrough else null,
                            )
                            if (t.dueDay >= 0) {
                                val overdue = !t.done && t.dueDay < today
                                val time = if (t.dueMinutes >= 0) " · ${fmtTime(t.dueMinutes)}" else ""
                                Text(
                                    (if (overdue) "Scaduto: " else "Scade: ") + fmtDay(t.dueDay) + time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (overdue) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (t.notes.isNotBlank()) {
                                Text(t.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                        }
                        Text(PRIORITIES[t.priority.coerceIn(0, 2)], style = MaterialTheme.typography.labelMedium, color = priorityColor(t.priority))
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editor = Task(title = "") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Nuovo task") }
    }

    editor?.let { t ->
        TaskEditor(
            initial = t,
            onDismiss = { editor = null },
            onSave = { vm.saveTask(it); editor = null },
            onDelete = if (t.id != 0L) ({ vm.deleteTask(t); editor = null }) else null,
        )
    }
}

@Composable
fun TaskEditor(initial: Task, onDismiss: () -> Unit, onSave: (Task) -> Unit, onDelete: (() -> Unit)?) {
    var title by remember { mutableStateOf(initial.title) }
    var notes by remember { mutableStateOf(initial.notes) }
    var dueDay by remember { mutableStateOf(initial.dueDay.takeIf { it >= 0 }) }
    var dueMinutes by remember { mutableStateOf(initial.dueMinutes) }
    var priority by remember { mutableStateOf(initial.priority) }
    var reminder by remember { mutableStateOf(initial.reminder) }

    FormDialog(
        title = if (initial.id == 0L) "Nuovo task" else "Modifica task",
        onDismiss = onDismiss,
        confirmEnabled = title.isNotBlank(),
        onDelete = onDelete,
        onConfirm = {
            onSave(
                initial.copy(
                    title = title.trim(), notes = notes.trim(), dueDay = dueDay ?: -1,
                    dueMinutes = if (dueDay == null) -1 else dueMinutes, priority = priority,
                    reminder = reminder && dueDay != null,
                ),
            )
        },
    ) {
        TextInput("Titolo", title, { title = it })
        TextInput("Note", notes, { notes = it }, singleLine = false)
        DatePickerField("Scadenza", dueDay, { dueDay = it; if (it == null) reminder = false }, clearable = true)
        if (dueDay != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeField("Ora", dueMinutes, { dueMinutes = it })
                if (dueMinutes >= 0) {
                    IconButton(onClick = { dueMinutes = -1 }) { Icon(Icons.Rounded.Close, contentDescription = "Rimuovi ora") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Promemoria", Modifier.weight(1f))
                Switch(checked = reminder, onCheckedChange = { reminder = it })
            }
        }
        Text("Priorità", style = MaterialTheme.typography.labelLarge)
        ChipRow(listOf(0, 1, 2), priority, { PRIORITIES[it] }) { priority = it }
    }
}
