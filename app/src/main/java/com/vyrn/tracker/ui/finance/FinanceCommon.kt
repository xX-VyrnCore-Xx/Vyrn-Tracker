package com.vyrn.tracker.ui.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.data.Account
import com.vyrn.tracker.data.Category
import com.vyrn.tracker.data.FinTx
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.data.centsToInput
import com.vyrn.tracker.data.fmtDay
import com.vyrn.tracker.data.fmtMonth
import com.vyrn.tracker.data.formatMoney
import com.vyrn.tracker.data.parseCents
import com.vyrn.tracker.ui.ChipRow
import com.vyrn.tracker.ui.DatePickerField
import com.vyrn.tracker.ui.DecimalField
import com.vyrn.tracker.ui.DropdownField
import com.vyrn.tracker.ui.EmojiBadge
import com.vyrn.tracker.ui.FormDialog
import com.vyrn.tracker.ui.TextInput
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.colorOf
import com.vyrn.tracker.ui.theme.ExpenseColor
import com.vyrn.tracker.ui.theme.IncomeColor
import java.time.YearMonth

@Composable
fun MonthSelector(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mese precedente") }
        Text(
            fmtMonth(month),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Mese successivo") }
    }
}

@Composable
fun AddButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
fun TxRow(tx: FinTx, accounts: Map<Long, Account>, categories: Map<Long, Category>, onClick: () -> Unit) {
    val cat = tx.categoryId?.let { categories[it] }
    val acc = accounts[tx.accountId]
    val to = tx.toAccountId?.let { accounts[it] }
    val (title, sub, amountText, amountColor) = when (tx.type) {
        TxType.INCOME -> Quad(
            tx.note.ifBlank { cat?.name ?: "Entrata" },
            listOfNotNull(cat?.name, acc?.name, fmtDay(tx.day)).joinToString(" · "),
            "+" + formatMoney(tx.amountCents), IncomeColor,
        )
        TxType.EXPENSE -> Quad(
            tx.note.ifBlank { cat?.name ?: "Spesa" },
            listOfNotNull(cat?.name, acc?.name, fmtDay(tx.day)).joinToString(" · "),
            "-" + formatMoney(tx.amountCents), ExpenseColor,
        )
        else -> Quad(
            tx.note.ifBlank { "Trasferimento" },
            "${acc?.name ?: "?"} → ${to?.name ?: "?"} · ${fmtDay(tx.day)}",
            formatMoney(tx.amountCents), MaterialTheme.colorScheme.onSurface,
        )
    }
    VCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(
                if (tx.type == TxType.TRANSFER) "🔁" else cat?.icon ?: if (tx.type == TxType.INCOME) "💰" else "💸",
                colorOf(cat?.colorIdx ?: 0), 40,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(amountText, color = amountColor, fontWeight = FontWeight.Bold)
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun TxEditor(
    initial: FinTx,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (FinTx) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var type by remember { mutableStateOf(initial.type) }
    var amount by remember { mutableStateOf(if (initial.amountCents > 0) centsToInput(initial.amountCents) else "") }
    var accountId by remember { mutableStateOf(initial.accountId) }
    var toAccountId by remember {
        mutableStateOf(initial.toAccountId ?: accounts.firstOrNull { it.id != initial.accountId }?.id)
    }
    var categoryId by remember { mutableStateOf(initial.categoryId) }
    var day by remember { mutableStateOf(initial.day) }
    var note by remember { mutableStateOf(initial.note) }

    val cents = parseCents(amount) ?: 0L
    val cats = categories.filter { it.isIncome == (type == TxType.INCOME) }
    val valid = cents > 0 && accounts.any { it.id == accountId } &&
        (type != TxType.TRANSFER || (toAccountId != null && toAccountId != accountId))

    FormDialog(
        title = if (initial.id == 0L) "Nuovo movimento" else "Modifica movimento",
        onDismiss = onDismiss,
        confirmEnabled = valid,
        onDelete = onDelete,
        onConfirm = {
            onSave(
                initial.copy(
                    type = type, amountCents = cents, accountId = accountId,
                    toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
                    categoryId = if (type == TxType.TRANSFER) null else categoryId,
                    day = day, note = note.trim(),
                ),
            )
        },
    ) {
        ChipRow(listOf(TxType.EXPENSE, TxType.INCOME, TxType.TRANSFER), type, {
            when (it) {
                TxType.EXPENSE -> "Uscita"
                TxType.INCOME -> "Entrata"
                else -> "Trasf."
            }
        }) { type = it; categoryId = null }
        DecimalField("Importo (€)", amount, { amount = it })
        if (accounts.isEmpty()) {
            Text("Crea prima un conto nella scheda Gestione.", color = MaterialTheme.colorScheme.error)
        } else {
            DropdownField(
                if (type == TxType.TRANSFER) "Da conto" else "Conto", accounts,
                accounts.firstOrNull { it.id == accountId }, { "${it.icon} ${it.name}" },
                { accountId = it.id },
            )
        }
        if (type == TxType.TRANSFER) {
            DropdownField(
                "A conto", accounts.filter { it.id != accountId },
                accounts.firstOrNull { it.id == toAccountId }, { "${it.icon} ${it.name}" },
                { toAccountId = it.id },
            )
        } else {
            DropdownField(
                "Categoria", cats, cats.firstOrNull { it.id == categoryId }, { "${it.icon} ${it.name}" },
                { categoryId = it.id },
            )
        }
        DatePickerField("Data", day, { if (it != null) day = it })
        TextInput("Nota", note, { note = it })
    }
}
