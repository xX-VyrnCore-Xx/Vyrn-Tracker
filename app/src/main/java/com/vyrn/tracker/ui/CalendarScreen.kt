package com.vyrn.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vyrn.tracker.RoutineViewModel
import com.vyrn.tracker.data.Habit
import com.vyrn.tracker.data.HabitLog
import com.vyrn.tracker.data.HabitLogic
import com.vyrn.tracker.data.Task
import com.vyrn.tracker.data.WEEKDAY_LETTER
import com.vyrn.tracker.data.fmtDayLong
import com.vyrn.tracker.data.fmtMonth
import com.vyrn.tracker.data.fmtTime
import com.vyrn.tracker.data.isWeekdayOn
import com.vyrn.tracker.ui.routine.TaskEditor
import com.vyrn.tracker.ui.routine.habitItems
import com.vyrn.tracker.ui.theme.ExpenseColor
import com.vyrn.tracker.ui.theme.IncomeColor
import com.vyrn.tracker.ui.theme.WarnColor
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(vm: RoutineViewModel = viewModel()) {
    val habits by vm.habits.collectAsState()
    val logs by vm.habitLogs.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val routines by vm.routines.collectAsState()
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var taskEditor by remember { mutableStateOf<Task?>(null) }

    val today = LocalDate.now()
    val ym = YearMonth.now().plusMonths(offset.toLong())
    val logMap = remember(logs) { logs.groupBy { it.habitId }.mapValues { e -> e.value.associateBy { it.day } } }
    val selDate = LocalDate.ofEpochDay(selected)
    val dayTasks = tasks.filter { it.dueDay == selected }
    val dayRoutines = routines.filter { isWeekdayOn(it.weekdaysMask, selDate) }

    ScreenScaffold("Calendario") {
        LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { offset-- }) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mese precedente") }
                        Text(
                            fmtMonth(ym),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = { offset = 0; selected = today.toEpochDay() }) { Text("Oggi") }
                        IconButton(onClick = { offset++ }) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Mese successivo") }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        WEEKDAY_LETTER.forEach {
                            Text(
                                it,
                                Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val lead = ym.atDay(1).dayOfWeek.value - 1
                    val rows = (lead + ym.lengthOfMonth() + 6) / 7
                    repeat(rows) { r ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { c ->
                                val dayNum = r * 7 + c - lead + 1
                                if (dayNum in 1..ym.lengthOfMonth()) {
                                    val date = ym.atDay(dayNum)
                                    DayCell(
                                        date = date,
                                        isSelected = date.toEpochDay() == selected,
                                        isToday = date == today,
                                        hasTask = tasks.any { !it.done && it.dueDay == date.toEpochDay() },
                                        habitState = habitState(habits, logMap, date, today),
                                        onClick = { selected = date.toEpochDay() },
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle(fmtDayLong(selected).replaceFirstChar { it.uppercase() })
            }
            if (dayTasks.isEmpty() && dayRoutines.isEmpty() && habits.none { HabitLogic.isScheduled(it, selDate) }) {
                item { EmptyState("🗓️", "Niente in programma per questo giorno.") }
            }
            if (dayTasks.isNotEmpty()) {
                items(dayTasks, key = { "t${it.id}" }) { t ->
                    VCard(onClick = { taskEditor = t }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = t.done, onCheckedChange = { vm.toggleTask(t) })
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                                )
                                if (t.dueMinutes >= 0) {
                                    Text(fmtTime(t.dueMinutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("Task", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            items(dayRoutines, key = { "r${it.id}" }) { r ->
                VCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(r.icon, colorOf(r.colorIdx), 36)
                        Text(r.name, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleSmall)
                        Text("Routine", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            habitItems(habits.filter { HabitLogic.isScheduled(it, selDate) }, logMap, selected, vm) { }
        }
    }

    taskEditor?.let { t ->
        TaskEditor(
            initial = t,
            onDismiss = { taskEditor = null },
            onSave = { vm.saveTask(it); taskEditor = null },
            onDelete = { vm.deleteTask(t); taskEditor = null },
        )
    }
}

/** 0 = niente, 1 = parziale, 2 = tutte completate. */
private fun habitState(habits: List<Habit>, logMap: Map<Long, Map<Long, HabitLog>>, date: LocalDate, today: LocalDate): Int {
    if (date.isAfter(today)) return 0
    val day = date.toEpochDay()
    val scheduled = habits.filter { it.createdDay <= day && HabitLogic.isScheduled(it, date) }
    if (scheduled.isEmpty()) return 0
    val done = scheduled.count { HabitLogic.isDone(it, logMap[it.id]?.get(day)) }
    return when {
        done == 0 -> 0
        done == scheduled.size -> 2
        else -> 1
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasTask: Boolean,
    habitState: Int,
    onClick: () -> Unit,
) {
    Box(
        Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
                if (hasTask) Box(Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.onPrimary else WarnColor))
                if (habitState > 0) {
                    Box(
                        Modifier.size(4.dp).clip(CircleShape)
                            .background(if (habitState == 2) IncomeColor else ExpenseColor.copy(alpha = 0.7f)),
                    )
                }
            }
        }
    }
}
