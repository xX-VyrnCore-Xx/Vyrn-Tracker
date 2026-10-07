package com.vyrn.tracker.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.FinanceViewModel
import com.vyrn.tracker.data.Debt
import com.vyrn.tracker.data.Recurring
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.data.centsToInput
import com.vyrn.tracker.data.fmtDay
import com.vyrn.tracker.data.formatMoney
import com.vyrn.tracker.data.parseCents
import com.vyrn.tracker.data.periodText
import com.vyrn.tracker.ui.ChipRow
import com.vyrn.tracker.ui.DatePickerField
import com.vyrn.tracker.ui.DecimalField
import com.vyrn.tracker.ui.DropdownField
import com.vyrn.tracker.ui.EmojiBadge
import com.vyrn.tracker.ui.EmptyState
import com.vyrn.tracker.ui.FormDialog
import com.vyrn.tracker.ui.ScreenPadding
import com.vyrn.tracker.ui.TextInput
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.colorOf
import com.vyrn.tracker.ui.theme.ExpenseColor
import com.vyrn.tracker.ui.theme.IncomeColor
import java.time.LocalDate

// ---------------- Spese ricorrenti e abbonamenti ----------------

@Composable
fun FinanceRecurring(vm: FinanceViewModel) {
    val list by vm.recurring.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    var editor by remember { mutableStateOf<Recurring?>(null) }
    val catMap = remember(categories) { categories.associateBy { it.id } }

    val monthlyOut = list.filter { it.active && it.type == TxType.EXPENSE }.sumOf {
        when (it.period) {
            0 -> it.amountCents * 52 / 12
            1 -> it.amountCents
            else -> it.amountCents / 12
        }
    }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AddButton("Nuova spesa ricorrente") {
                editor = Recurring(
                    name = "", amountCents = 0, accountId = accounts.firstOrNull()?.id ?: 0L,
                    nextDay = LocalDate.now().toEpochDay(), anchor = LocalDate.now().dayOfMonth,
                )
            }
        }
        if (list.isNotEmpty()) {
            item {
                VCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Costo mensile stimato", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        formatMoney(monthlyOut),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        } else {
            item { EmptyState("🔁", "Nessuna ricorrenza.\nAggiungi abbonamenti, affitto o stipendio: i movimenti verranno creati in automatico.") }
        }
        items(list, key = { it.id }) { r ->
            val cat = r.categoryId?.let { catMap[it] }
            VCard(modifier = Modifier.alpha(if (r.active) 1f else 0.5f), onClick = { editor = r }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(cat?.icon ?: "🔁", colorOf(cat?.colorIdx ?: 0), 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${periodText(r.period)} · prossima: ${fmtDay(r.nextDay)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            (if (r.type == TxType.INCOME) "+" else "-") + formatMoney(r.amountCents),
                            fontWeight = FontWeight.Bold,
                            color = if (r.type == TxType.INCOME) IncomeColor else ExpenseColor,
                        )
                        Switch(checked = r.active, onCheckedChange = { vm.setRecurringActive(r, it) })
                    }
                }
            }
        }
    }

    editor?.let { r ->
        var name by remember(r) { mutableStateOf(r.name) }
        var type by remember(r) { mutableStateOf(r.type) }
        var amount by remember(r) { mutableStateOf(if (r.amountCents > 0) centsToInput(r.amountCents) else "") }
        var accountId by remember(r) { mutableStateOf(r.accountId) }
        var categoryId by remember(r) { mutableStateOf(r.categoryId) }
        var period by remember(r) { mutableStateOf(r.period) }
        var nextDay by remember(r) { mutableStateOf(r.nextDay) }
        val cents = parseCents(amount) ?: 0L
        val cats = categories.filter { it.isIncome == (type == TxType.INCOME) }
        FormDialog(
            title = if (r.id == 0L) "Nuova ricorrenza" else "Modifica ricorrenza",
            onDismiss = { editor = null },
            confirmEnabled = name.isNotBlank() && cents > 0 && accounts.any { it.id == accountId },
            onDelete = if (r.id != 0L) ({ vm.deleteRecurring(r); editor = null }) else null,
            onConfirm = {
                vm.saveRecurring(
                    r.copy(
                        name = name.trim(), type = type, amountCents = cents, accountId = accountId,
                        categoryId = categoryId, period = period, nextDay = nextDay,
                        anchor = LocalDate.ofEpochDay(nextDay).dayOfMonth,
                    ),
                )
                editor = null
            },
        ) {
            TextInput("Nome (es. Netflix, Affitto)", name, { name = it })
            ChipRow(listOf(TxType.EXPENSE, TxType.INCOME), type, { if (it == TxType.EXPENSE) "Uscita" else "Entrata" }) {
                type = it
                categoryId = null
            }
            DecimalField("Importo (€)", amount, { amount = it })
            DropdownField("Conto", accounts, accounts.firstOrNull { it.id == accountId }, { "${it.icon} ${it.name}" }, { accountId = it.id })
            DropdownField("Categoria", cats, cats.firstOrNull { it.id == categoryId }, { "${it.icon} ${it.name}" }, { categoryId = it.id })
            ChipRow(listOf(0, 1, 2), period, { periodText(it) }) { period = it }
            DatePickerField("Prossima scadenza", nextDay, { if (it != null) nextDay = it })
        }
    }
}

// ---------------- Debiti e prestiti ----------------

@Composable
fun FinanceDebts(vm: FinanceViewModel) {
    val debts by vm.debts.collectAsState()
    var editor by remember { mutableStateOf<Debt?>(null) }
    val today = LocalDate.now().toEpochDay()

    val owedToMe = debts.filter { it.direction == 0 }.sumOf { it.totalCents - it.paidCents }
    val iOwe = debts.filter { it.direction == 1 }.sumOf { it.totalCents - it.paidCents }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { AddButton("Nuovo debito / prestito") { editor = Debt(person = "", totalCents = 0) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VCard(Modifier.weight(1f)) {
                    Text("Mi devono", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(owedToMe), fontWeight = FontWeight.Bold, color = IncomeColor, style = MaterialTheme.typography.titleMedium)
                }
                VCard(Modifier.weight(1f)) {
                    Text("Devo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(iOwe), fontWeight = FontWeight.Bold, color = ExpenseColor, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (debts.isEmpty()) {
            item { EmptyState("🤝", "Nessun debito o prestito registrato.") }
        }
        items(debts, key = { it.id }) { d ->
            val remaining = d.totalCents - d.paidCents
            val ratio = if (d.totalCents > 0) d.paidCents.toFloat() / d.totalCents else 0f
            val color = if (d.direction == 0) IncomeColor else ExpenseColor
            VCard(modifier = Modifier.alpha(if (remaining <= 0) 0.55f else 1f), onClick = { editor = d }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(if (d.direction == 0) "📥" else "📤", color, 40)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.person, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val due = if (d.dueDay >= 0) {
                            val late = remaining > 0 && d.dueDay < today
                            (if (late) "Scaduto il " else "Entro il ") + fmtDay(d.dueDay)
                        } else if (d.direction == 0) "Ti deve dei soldi" else "Devi dei soldi"
                        Text(due, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (remaining <= 0) "Saldato" else formatMoney(remaining),
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = color,
                )
                Text(
                    "${formatMoney(d.paidCents)} di ${formatMoney(d.totalCents)}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    editor?.let { d ->
        var person by remember(d) { mutableStateOf(d.person) }
        var direction by remember(d) { mutableStateOf(d.direction) }
        var total by remember(d) { mutableStateOf(if (d.totalCents > 0) centsToInput(d.totalCents) else "") }
        var paid by remember(d) { mutableStateOf(if (d.paidCents > 0) centsToInput(d.paidCents) else "") }
        var note by remember(d) { mutableStateOf(d.note) }
        var due by remember(d) { mutableStateOf(d.dueDay.takeIf { it >= 0 }) }
        val totalCents = parseCents(total) ?: 0L
        FormDialog(
            title = if (d.id == 0L) "Nuovo debito / prestito" else "Modifica",
            onDismiss = { editor = null },
            confirmEnabled = person.isNotBlank() && totalCents > 0,
            onDelete = if (d.id != 0L) ({ vm.deleteDebt(d); editor = null }) else null,
            onConfirm = {
                vm.saveDebt(
                    d.copy(
                        person = person.trim(), direction = direction, totalCents = totalCents,
                        paidCents = (parseCents(paid) ?: 0L).coerceIn(0L, totalCents), note = note.trim(),
                        dueDay = due ?: -1,
                    ),
                )
                editor = null
            },
        ) {
            ChipRow(listOf(0, 1), direction, { if (it == 0) "Mi devono" else "Devo io" }) { direction = it }
            TextInput("Persona", person, { person = it })
            DecimalField("Importo totale (€)", total, { total = it })
            DecimalField("Già saldato (€)", paid, { paid = it })
            DatePickerField("Scadenza", due, { due = it }, clearable = true)
            TextInput("Nota", note, { note = it })
        }
    }
}
