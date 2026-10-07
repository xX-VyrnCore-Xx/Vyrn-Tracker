@file:OptIn(ExperimentalMaterial3Api::class)

package com.vyrn.tracker.ui.routine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.RoutineViewModel
import com.vyrn.tracker.data.Habit
import com.vyrn.tracker.data.HabitLog
import com.vyrn.tracker.data.HabitLogic
import com.vyrn.tracker.data.WEEKDAY_LETTER
import com.vyrn.tracker.data.fmtDayLong
import com.vyrn.tracker.data.fmtMonth
import com.vyrn.tracker.data.formatNumber
import com.vyrn.tracker.data.parseDoubleIt
import com.vyrn.tracker.ui.VProgress
import com.vyrn.tracker.ui.ChipRow
import com.vyrn.tracker.ui.ColorPicker
import com.vyrn.tracker.ui.DecimalField
import com.vyrn.tracker.ui.EmojiBadge
import com.vyrn.tracker.ui.EmojiPicker
import com.vyrn.tracker.ui.EmptyState
import com.vyrn.tracker.ui.FormDialog
import com.vyrn.tracker.ui.HabitEmojis
import com.vyrn.tracker.ui.ReminderField
import com.vyrn.tracker.ui.ScreenPadding
import com.vyrn.tracker.ui.TextInput
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.WeekdayChips
import com.vyrn.tracker.ui.colorOf
import com.vyrn.tracker.ui.theme.ExpenseColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun HabitsTab(vm: RoutineViewModel) {
    val habits by vm.habits.collectAsState()
    val logs by vm.habitLogs.collectAsState()
    val selected by vm.selectedDay.collectAsState()
    val logMap = remember(logs) { logs.groupBy { it.habitId }.mapValues { e -> e.value.associateBy { it.day } } }
    var editor by remember { mutableStateOf<Habit?>(null) }
    var detailId by remember { mutableStateOf<Long?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { WeekStrip(selected) { vm.selectedDay.value = it } }
            item { DaySummary(habits, logMap, selected) }
            if (habits.isEmpty()) {
                item { EmptyState("🌱", "Nessuna abitudine.\nTocca + per crearne una.") }
            }
            habitItems(habits, logMap, selected, vm) { detailId = it.id }
        }
        FloatingActionButton(
            onClick = { editor = Habit(name = "") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Nuova abitudine") }
    }

    editor?.let { h ->
        HabitEditor(
            initial = h,
            onDismiss = { editor = null },
            onSave = { vm.saveHabit(it); editor = null },
            onDelete = if (h.id != 0L) ({ vm.deleteHabit(h); editor = null }) else null,
        )
    }

    detailId?.let { id ->
        val h = habits.firstOrNull { it.id == id }
        if (h == null) {
            detailId = null
        } else {
            HabitDetailDialog(
                habit = h,
                day = selected,
                log = logMap[h.id]?.get(selected),
                done = HabitLogic.doneDays(h, logMap[h.id].orEmpty()),
                onSaveNote = { vm.setHabitNote(h, selected, it); detailId = null },
                onEdit = { editor = h; detailId = null },
                onDismiss = { detailId = null },
            )
        }
    }
}

fun LazyListScope.habitItems(
    habits: List<Habit>,
    logMap: Map<Long, Map<Long, HabitLog>>,
    day: Long,
    vm: RoutineViewModel,
    onOpen: (Habit) -> Unit,
) {
    val date = LocalDate.ofEpochDay(day)
    items(habits, key = { it.id }) { h ->
        val habitLogs = logMap[h.id].orEmpty()
        val log = habitLogs[day]
        val doneDays = remember(h, habitLogs) { HabitLogic.doneDays(h, habitLogs) }
        HabitCard(
            h = h,
            log = log,
            streak = HabitLogic.streak(h, doneDays, LocalDate.now()),
            weekDone = HabitLogic.weekCount(doneDays, date),
            scheduled = HabitLogic.isScheduled(h, date),
            onToggle = { vm.toggleHabit(h, day, HabitLogic.isDone(h, log)) },
            onDelta = { delta -> vm.setHabitValue(h, day, (log?.value ?: 0.0) + delta) },
            onClick = { onOpen(h) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun WeekStrip(selected: Long, onSelect: (Long) -> Unit) {
    val sel = LocalDate.ofEpochDay(selected)
    val start = sel.minusDays((sel.dayOfWeek.value - 1).toLong())
    val today = LocalDate.now()
    VCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelect(sel.minusWeeks(1).toEpochDay()) }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Settimana precedente")
            }
            Text(
                fmtMonth(YearMonth.from(sel)),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onSelect(today.toEpochDay()) }) { Text("Oggi") }
            IconButton(onClick = { onSelect(sel.plusWeeks(1).toEpochDay()) }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Settimana successiva")
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..6) {
                val d = start.plusDays(i.toLong())
                val isSel = d == sel
                val isToday = d == today
                Column(
                    Modifier.clip(MaterialTheme.shapes.medium).clickable { onSelect(d.toEpochDay()) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(WEEKDAY_LETTER[i], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                            .then(if (isToday && !isSel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            d.dayOfMonth.toString(),
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySummary(habits: List<Habit>, logMap: Map<Long, Map<Long, HabitLog>>, day: Long) {
    val date = LocalDate.ofEpochDay(day)
    val scheduled = habits.filter { HabitLogic.isScheduled(it, date) }
    val done = scheduled.count { HabitLogic.isDone(it, logMap[it.id]?.get(day)) }
    val progress = if (scheduled.isEmpty()) 0f else done.toFloat() / scheduled.size
    val perfect = scheduled.isNotEmpty() && done == scheduled.size
    val hour = LocalTime.now().hour
    val greeting = when {
        date != LocalDate.now() -> fmtDayLong(day).replaceFirstChar { it.uppercase() }
        hour < 5 -> "Buonanotte"
        hour < 12 -> "Buongiorno"
        hour < 18 -> "Buon pomeriggio"
        else -> "Buonasera"
    }
    VCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(greeting, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.height(4.dp))
        Text(
            if (scheduled.isEmpty()) "Nessuna abitudine prevista" else "$done su ${scheduled.size} completate",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        AnimatedVisibility(visible = perfect, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(
                "🎉 Giornata perfetta!",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        VProgress(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
        )
    }
}

@Composable
fun HabitCard(
    h: Habit,
    log: HabitLog?,
    streak: Int,
    weekDone: Int,
    scheduled: Boolean,
    onToggle: () -> Unit,
    onDelta: (Double) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = colorOf(h.colorIdx)
    val done = HabitLogic.isDone(h, log)
    val checkColor by animateColorAsState(
        if (done) color else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "checkColor",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (done) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkIcon",
    )
    VCard(modifier = modifier.alpha(if (scheduled) 1f else 0.55f), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(h.icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val freq = if (h.freqType == 2) "${HabitLogic.frequencyText(h)} ($weekDone/${h.timesPerWeek})" else HabitLogic.frequencyText(h)
                Text(
                    if (scheduled) freq else "$freq · non previsto oggi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (streak > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        Text(" $streak ${HabitLogic.streakUnit(h)}", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (h.kind != 1) {
                Box(
                    Modifier.size(42.dp).scale(checkScale).clip(CircleShape)
                        .background(checkColor)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Completata",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.scale(iconScale).alpha(iconScale.coerceIn(0f, 1f)),
                    )
                    if (h.kind == 2 && !done) {
                        Icon(Icons.Rounded.Close, contentDescription = "Ricaduta", tint = ExpenseColor)
                    }
                }
            } else {
                val step = HabitLogic.step(h)
                IconButton(onClick = { onDelta(-step) }) { Icon(Icons.Rounded.Remove, contentDescription = "Meno") }
                IconButton(onClick = { onDelta(step) }) { Icon(Icons.Rounded.Add, contentDescription = "Più") }
            }
        }
        if (h.kind == 1) {
            val value = log?.value ?: 0.0
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                VProgress(
                    progress = { (value / h.target).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                    color = color,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${formatNumber(value)}/${formatNumber(h.target)} ${h.unit}".trim(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (!log?.note.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("📝 ${log?.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HabitDetailDialog(
    habit: Habit,
    day: Long,
    log: HabitLog?,
    done: Set<Long>,
    onSaveNote: (String) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf(log?.note.orEmpty()) }
    val today = LocalDate.now()
    val streak = HabitLogic.streak(habit, done, today)
    val last30 = (0..29).count { today.minusDays(it.toLong()).toEpochDay() in done }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${habit.icon} ${habit.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(fmtDayLong(day).replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Serie attuale: $streak ${HabitLogic.streakUnit(habit)}")
                Text("Completamenti totali: ${done.size}")
                Text("Ultimi 30 giorni: $last30")
                TextInput("Nota del giorno", note, { note = it }, singleLine = false)
            }
        },
        confirmButton = { TextButton(onClick = { onSaveNote(note) }) { Text("Salva nota") } },
        dismissButton = {
            Row {
                TextButton(onClick = onEdit) { Text("Modifica") }
                TextButton(onClick = onDismiss) { Text("Chiudi") }
            }
        },
    )
}

@Composable
private fun HabitEditor(initial: Habit, onDismiss: () -> Unit, onSave: (Habit) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var colorIdx by remember { mutableStateOf(initial.colorIdx) }
    var kind by remember { mutableStateOf(initial.kind) }
    var target by remember { mutableStateOf(formatNumber(initial.target)) }
    var unit by remember { mutableStateOf(initial.unit) }
    var freqType by remember { mutableStateOf(initial.freqType) }
    var mask by remember { mutableStateOf(initial.weekdaysMask) }
    var times by remember { mutableStateOf(initial.timesPerWeek) }
    var reminder by remember { mutableStateOf(initial.reminderMinutes) }

    val targetValue = if (kind != 1) 1.0 else parseDoubleIt(target) ?: 0.0
    FormDialog(
        title = if (initial.id == 0L) "Nuova abitudine" else "Modifica abitudine",
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && targetValue > 0 && (kind == 2 || freqType != 1 || mask != 0),
        onDelete = onDelete,
        onConfirm = {
            onSave(
                initial.copy(
                    name = name.trim(), icon = icon, colorIdx = colorIdx, kind = kind, target = targetValue,
                    unit = if (kind == 1) unit.trim() else "", freqType = if (kind == 2) 0 else freqType, weekdaysMask = mask,
                    timesPerWeek = times, reminderMinutes = reminder,
                ),
            )
        },
    ) {
        TextInput("Nome", name, { name = it })
        EmojiPicker(HabitEmojis, icon) { icon = it }
        ColorPicker(colorIdx) { colorIdx = it }
        Text("Tipo", style = MaterialTheme.typography.labelLarge)
        ChipRow(listOf(0, 1, 2), kind, { listOf("Sì / No", "Quantità", "Da evitare")[it] }) { kind = it }
        if (kind == 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField("Obiettivo", target, { target = it }, Modifier.weight(1f))
                TextInput("Unità", unit, { unit = it }, Modifier.weight(1f))
            }
        }
        if (kind == 2) {
            Text(
                "Conta i giorni liberi: tocca la spunta per segnare una ricaduta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Frequenza", style = MaterialTheme.typography.labelLarge)
            ChipRow(listOf(0, 1, 2), freqType, { listOf("Ogni giorno", "Giorni", "Settimana")[it] }) { freqType = it }
            if (freqType == 1) WeekdayChips(mask) { mask = it }
            if (freqType == 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Volte a settimana", Modifier.weight(1f))
                    IconButton(onClick = { times = (times - 1).coerceAtLeast(1) }) { Icon(Icons.Rounded.Remove, null) }
                    Text(times.toString(), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { times = (times + 1).coerceAtMost(7) }) { Icon(Icons.Rounded.Add, null) }
                }
            }
        }
        ReminderField(reminder) { reminder = it }
    }
}
