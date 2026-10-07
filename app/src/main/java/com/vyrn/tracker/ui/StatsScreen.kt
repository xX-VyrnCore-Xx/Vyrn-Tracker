package com.vyrn.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vyrn.tracker.FinanceViewModel
import com.vyrn.tracker.RoutineViewModel
import com.vyrn.tracker.data.HabitLogic
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.data.WEEKDAY_LETTER
import com.vyrn.tracker.data.fmtMonthShort
import com.vyrn.tracker.data.formatMoney
import com.vyrn.tracker.data.monthRange
import com.vyrn.tracker.ui.finance.MonthSelector
import com.vyrn.tracker.ui.theme.ExpenseColor
import com.vyrn.tracker.ui.theme.IncomeColor
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun StatsScreen(rvm: RoutineViewModel = viewModel(), fvm: FinanceViewModel = viewModel()) {
    val habits by rvm.habits.collectAsState()
    val logs by rvm.habitLogs.collectAsState()
    val tasks by rvm.tasks.collectAsState()
    val month by fvm.month.collectAsState()
    val txs by fvm.txs.collectAsState()
    val monthTxs by fvm.monthTxs.collectAsState()
    val categories by fvm.categories.collectAsState()

    val today = LocalDate.now()
    val logMap = remember(logs) { logs.groupBy { it.habitId }.mapValues { e -> e.value.associateBy { it.day } } }

    // Abitudini: completamento negli ultimi 7 giorni
    val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val percents = last7.map { d ->
        val day = d.toEpochDay()
        val scheduled = habits.filter { it.createdDay <= day && HabitLogic.isScheduled(it, d) }
        if (scheduled.isEmpty()) 0f
        else scheduled.count { HabitLogic.isDone(it, logMap[it.id]?.get(day)) }.toFloat() / scheduled.size * 100f
    }
    val bestStreak = habits.maxOfOrNull { h ->
        HabitLogic.streak(h, HabitLogic.doneDays(h, logMap[h.id].orEmpty()), today)
    } ?: 0
    val todayScheduled = habits.filter { HabitLogic.isScheduled(it, today) }
    val todayDone = todayScheduled.count { HabitLogic.isDone(it, logMap[it.id]?.get(today.toEpochDay())) }

    // Task
    val tasksDone = tasks.count { it.done }
    val tasksOpen = tasks.count { !it.done }
    val overdue = tasks.count { !it.done && it.dueDay in 0 until today.toEpochDay() }

    // Finanza
    val income = monthTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
    val expense = monthTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val slices = monthTxs.filter { it.type == TxType.EXPENSE }
        .groupBy { it.categoryId }
        .map { (id, list) ->
            val c = id?.let { catMap[it] }
            Slice(c?.name ?: "Senza categoria", list.sumOf { it.amountCents }.toFloat(), colorOf(c?.colorIdx ?: 7))
        }
        .sortedByDescending { it.value }
    val months = (5 downTo 0).map { month.minusMonths(it.toLong()) }
    val monthIncome = months.map { m -> val r = monthRange(m); txs.filter { it.type == TxType.INCOME && it.day in r }.sumOf { it.amountCents } / 100f }
    val monthExpense = months.map { m -> val r = monthRange(m); txs.filter { it.type == TxType.EXPENSE && it.day in r }.sumOf { it.amountCents } / 100f }

    // Vista annuale (anno del mese selezionato)
    val year = month.year
    val yearMonths = (1..12).map { YearMonth.of(year, it) }
    val yearIncomeByMonth = yearMonths.map { m -> val r = monthRange(m); txs.filter { it.type == TxType.INCOME && it.day in r }.sumOf { it.amountCents } }
    val yearExpenseByMonth = yearMonths.map { m -> val r = monthRange(m); txs.filter { it.type == TxType.EXPENSE && it.day in r }.sumOf { it.amountCents } }
    val yearIncome = yearIncomeByMonth.sum()
    val yearExpense = yearExpenseByMonth.sum()
    val yearTop = txs.filter { it.type == TxType.EXPENSE && LocalDate.ofEpochDay(it.day).year == year }
        .groupBy { it.categoryId }
        .map { (id, list) -> (id?.let { catMap[it] }) to list.sumOf { it.amountCents } }
        .sortedByDescending { it.second }
        .take(5)
    var scope by rememberSaveable { mutableIntStateOf(0) }

    ScreenScaffold("Statistiche") {
        LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("Routine") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Oggi", "$todayDone/${todayScheduled.size}", Modifier.weight(1f))
                    StatTile("Serie migliore", "🔥 $bestStreak", Modifier.weight(1f))
                    StatTile("Task fatti", "$tasksDone", Modifier.weight(1f))
                }
            }
            item {
                VCard {
                    Text("Abitudini completate (ultimi 7 giorni)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    BarChart(
                        series = listOf(BarSeries(MaterialTheme.colorScheme.primary, percents)),
                        labels = last7.map { WEEKDAY_LETTER[it.dayOfWeek.value - 1] },
                        maxValue = 100f,
                    )
                    val avg = if (percents.isEmpty()) 0 else percents.average().roundToInt()
                    Text("Media: $avg%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Task aperti", "$tasksOpen", Modifier.weight(1f))
                    StatTile("In ritardo", "$overdue", Modifier.weight(1f), if (overdue > 0) ExpenseColor else null)
                    StatTile("Completati", "$tasksDone", Modifier.weight(1f))
                }
            }

            item { SectionTitle("Finanza") }
            item { ChipRow(listOf(0, 1), scope, { if (it == 0) "Mese" else "Anno" }) { scope = it } }
            if (scope == 0) {
                item { MonthSelector(month, fvm::previousMonth, fvm::nextMonth) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Entrate", formatMoney(income), Modifier.weight(1f), IncomeColor)
                        StatTile("Uscite", formatMoney(expense), Modifier.weight(1f), ExpenseColor)
                        StatTile("Risparmio", formatMoney(income - expense), Modifier.weight(1f))
                    }
                }
                item {
                    VCard {
                        Text("Spese per categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        if (slices.isEmpty()) {
                            Text("Nessuna spesa in questo mese.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DonutChart(slices, Modifier.size(140.dp)) {
                                    Text(formatMoney(expense), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    slices.take(6).forEach { s ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "${s.label} · ${formatMoney(s.value.toLong())}",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    VCard {
                        Text("Entrate e uscite (6 mesi)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        BarChart(
                            series = listOf(BarSeries(IncomeColor, monthIncome), BarSeries(ExpenseColor, monthExpense)),
                            labels = months.map { fmtMonthShort(it) },
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(IncomeColor))
                            Text("  Entrate    ", style = MaterialTheme.typography.labelMedium)
                            Box(Modifier.size(10.dp).clip(CircleShape).background(ExpenseColor))
                            Text("  Uscite", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

            } else {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { fvm.month.value = month.minusYears(1) }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Anno precedente")
                        }
                        Text(
                            "$year",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = { fvm.month.value = month.plusYears(1) }) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Anno successivo")
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Entrate", formatMoney(yearIncome), Modifier.weight(1f), IncomeColor)
                        StatTile("Uscite", formatMoney(yearExpense), Modifier.weight(1f), ExpenseColor)
                        StatTile("Risparmio", formatMoney(yearIncome - yearExpense), Modifier.weight(1f))
                    }
                }
                item {
                    VCard {
                        Text("Mese per mese", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        BarChart(
                            series = listOf(
                                BarSeries(IncomeColor, yearIncomeByMonth.map { it / 100f }),
                                BarSeries(ExpenseColor, yearExpenseByMonth.map { it / 100f }),
                            ),
                            labels = yearMonths.map { fmtMonthShort(it).take(1) },
                        )
                        Spacer(Modifier.height(6.dp))
                        val avg = yearExpense / 12
                        Text(
                            "Spesa media mensile: ${formatMoney(avg)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    VCard {
                        Text("Categorie più costose dell'anno", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        if (yearTop.isEmpty()) {
                            Text("Nessuna spesa registrata.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        yearTop.forEach { (cat, cents) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${cat?.icon ?: "🏷️"} ${cat?.name ?: "Senza categoria"}", Modifier.weight(1f), maxLines = 1)
                                Text(formatMoney(cents), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    VCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
