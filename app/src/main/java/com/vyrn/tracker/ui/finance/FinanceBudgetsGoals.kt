package com.vyrn.tracker.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.FinanceViewModel
import com.vyrn.tracker.data.Budget
import com.vyrn.tracker.data.Category
import com.vyrn.tracker.data.Goal
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.data.centsToInput
import com.vyrn.tracker.data.fmtDay
import com.vyrn.tracker.data.formatMoney
import com.vyrn.tracker.data.parseCents
import com.vyrn.tracker.ui.ChipRow
import com.vyrn.tracker.ui.ColorPicker
import com.vyrn.tracker.ui.DatePickerField
import com.vyrn.tracker.ui.DecimalField
import com.vyrn.tracker.ui.DropdownField
import com.vyrn.tracker.ui.EmojiBadge
import com.vyrn.tracker.ui.EmojiPicker
import com.vyrn.tracker.ui.EmptyState
import com.vyrn.tracker.ui.FormDialog
import com.vyrn.tracker.ui.GoalEmojis
import com.vyrn.tracker.ui.ScreenPadding
import com.vyrn.tracker.ui.TextInput
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.colorOf
import com.vyrn.tracker.ui.theme.ExpenseColor
import com.vyrn.tracker.ui.theme.IncomeColor
import com.vyrn.tracker.ui.theme.WarnColor
import java.time.LocalDate

// ---------------- Budget ----------------

private data class BudgetEdit(val categoryId: Long?, val limit: String, val isNew: Boolean)

@Composable
fun FinanceBudgets(vm: FinanceViewModel) {
    val month by vm.month.collectAsState()
    val categories by vm.categories.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val monthTxs by vm.monthTxs.collectAsState()
    var editor by remember { mutableStateOf<BudgetEdit?>(null) }

    val spentByCat = remember(monthTxs) {
        monthTxs.filter { it.type == TxType.EXPENSE }.groupBy { it.categoryId }.mapValues { e -> e.value.sumOf { it.amountCents } }
    }
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val totalLimit = budgets.sumOf { it.limitCents }
    val totalSpent = budgets.sumOf { spentByCat[it.categoryId] ?: 0L }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { MonthSelector(month, vm::previousMonth, vm::nextMonth) }
        item { AddButton("Nuovo budget") { editor = BudgetEdit(null, "", true) } }
        if (budgets.isNotEmpty()) {
            item {
                VCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Budget totale del mese", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "${formatMoney(totalSpent)} / ${formatMoney(totalLimit)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        } else {
            item { EmptyState("🎯", "Nessun budget.\nImposta un limite mensile per le tue categorie di spesa.") }
        }
        items(budgets, key = { it.categoryId }) { b ->
            val cat = catMap[b.categoryId]
            val spent = spentByCat[b.categoryId] ?: 0L
            val ratio = if (b.limitCents > 0) spent.toFloat() / b.limitCents else 0f
            val barColor = when {
                ratio > 1f -> ExpenseColor
                ratio > 0.8f -> WarnColor
                else -> IncomeColor
            }
            VCard(onClick = { editor = BudgetEdit(b.categoryId, centsToInput(b.limitCents), false) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(cat?.icon ?: "🏷️", colorOf(cat?.colorIdx ?: 0), 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cat?.name ?: "Categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (ratio > 1f) "Superato di ${formatMoney(spent - b.limitCents)}" else "Restano ${formatMoney(b.limitCents - spent)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ratio > 1f) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("${formatMoney(spent)} / ${formatMoney(b.limitCents)}", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = barColor,
                )
            }
        }
    }

    editor?.let { e ->
        val available: List<Category> =
            if (e.isNew) categories.filter { c -> !c.isIncome && budgets.none { it.categoryId == c.id } }
            else categories.filter { it.id == e.categoryId }
        var catId by remember(e) { mutableStateOf(e.categoryId) }
        var limit by remember(e) { mutableStateOf(e.limit) }
        val cents = parseCents(limit) ?: 0L
        FormDialog(
            title = if (e.isNew) "Nuovo budget" else "Modifica budget",
            onDismiss = { editor = null },
            confirmEnabled = catId != null && cents > 0,
            onDelete = if (!e.isNew) ({ e.categoryId?.let { vm.deleteBudget(it) }; editor = null }) else null,
            onConfirm = { catId?.let { vm.saveBudget(it, cents) }; editor = null },
        ) {
            if (available.isEmpty() && e.isNew) {
                Text("Tutte le categorie di spesa hanno già un budget.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownField(
                "Categoria", available, available.firstOrNull { it.id == catId }, { "${it.icon} ${it.name}" },
                { if (e.isNew) catId = it.id },
            )
            DecimalField("Limite mensile (€)", limit, { limit = it })
        }
    }
}

// ---------------- Obiettivi ----------------

@Composable
fun FinanceGoals(vm: FinanceViewModel) {
    val goals by vm.goals.collectAsState()
    var editor by remember { mutableStateOf<Goal?>(null) }
    var funds by remember { mutableStateOf<Goal?>(null) }
    val today = LocalDate.now().toEpochDay()

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { AddButton("Nuovo obiettivo di risparmio") { editor = Goal(name = "", targetCents = 0) } }
        if (goals.isEmpty()) {
            item { EmptyState("🐷", "Nessun obiettivo.\nCrea un salvadanaio per un viaggio, un acquisto o un fondo emergenze.") }
        }
        items(goals, key = { it.id }) { g ->
            val color = colorOf(g.colorIdx)
            val ratio = if (g.targetCents > 0) g.savedCents.toFloat() / g.targetCents else 0f
            VCard(onClick = { editor = g }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(g.icon, color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val deadline = if (g.deadlineDay >= 0) {
                            val late = g.deadlineDay < today && ratio < 1f
                            (if (late) "Scaduto il " else "Entro il ") + fmtDay(g.deadlineDay)
                        } else "Senza scadenza"
                        Text(deadline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${(ratio * 100).toInt()}%", fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = color,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${formatMoney(g.savedCents)} di ${formatMoney(g.targetCents)}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { funds = g }) { Text("Versa / Preleva") }
                }
            }
        }
    }

    editor?.let { g ->
        var name by remember(g) { mutableStateOf(g.name) }
        var icon by remember(g) { mutableStateOf(g.icon) }
        var colorIdx by remember(g) { mutableStateOf(g.colorIdx) }
        var target by remember(g) { mutableStateOf(if (g.targetCents > 0) centsToInput(g.targetCents) else "") }
        var saved by remember(g) { mutableStateOf(if (g.savedCents > 0) centsToInput(g.savedCents) else "") }
        var deadline by remember(g) { mutableStateOf(g.deadlineDay.takeIf { it >= 0 }) }
        val targetCents = parseCents(target) ?: 0L
        FormDialog(
            title = if (g.id == 0L) "Nuovo obiettivo" else "Modifica obiettivo",
            onDismiss = { editor = null },
            confirmEnabled = name.isNotBlank() && targetCents > 0,
            onDelete = if (g.id != 0L) ({ vm.deleteGoal(g); editor = null }) else null,
            onConfirm = {
                vm.saveGoal(
                    g.copy(
                        name = name.trim(), icon = icon, colorIdx = colorIdx, targetCents = targetCents,
                        savedCents = (parseCents(saved) ?: 0L).coerceAtLeast(0L), deadlineDay = deadline ?: -1,
                    ),
                )
                editor = null
            },
        ) {
            TextInput("Nome", name, { name = it })
            EmojiPicker(GoalEmojis, icon) { icon = it }
            ColorPicker(colorIdx) { colorIdx = it }
            DecimalField("Obiettivo (€)", target, { target = it })
            DecimalField("Già risparmiato (€)", saved, { saved = it })
            DatePickerField("Scadenza", deadline, { deadline = it }, clearable = true)
        }
    }

    funds?.let { g ->
        var amount by remember(g) { mutableStateOf("") }
        var deposit by remember(g) { mutableStateOf(true) }
        val cents = parseCents(amount) ?: 0L
        FormDialog(
            title = g.name,
            onDismiss = { funds = null },
            confirmText = if (deposit) "Versa" else "Preleva",
            confirmEnabled = cents > 0,
            onConfirm = { vm.changeGoalFunds(g, if (deposit) cents else -cents); funds = null },
        ) {
            ChipRow(listOf(true, false), deposit, { if (it) "Versa" else "Preleva" }) { deposit = it }
            DecimalField("Importo (€)", amount, { amount = it })
        }
    }
}
