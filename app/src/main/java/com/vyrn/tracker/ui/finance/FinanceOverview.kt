package com.vyrn.tracker.ui.finance

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.FinanceViewModel
import com.vyrn.tracker.data.FinTx
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.data.formatMoney
import com.vyrn.tracker.ui.DonutChart
import com.vyrn.tracker.ui.EmojiBadge
import com.vyrn.tracker.ui.EmptyState
import com.vyrn.tracker.ui.ScreenPadding
import com.vyrn.tracker.ui.SectionTitle
import com.vyrn.tracker.ui.Slice
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.colorOf
import com.vyrn.tracker.ui.theme.ExpenseColor
import com.vyrn.tracker.ui.theme.IncomeColor
import kotlin.math.roundToInt

@Composable
fun FinanceOverview(vm: FinanceViewModel, onEditTx: (FinTx) -> Unit, onSeeAll: () -> Unit) {
    val month by vm.month.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    val balances by vm.balances.collectAsState()
    val monthTxs by vm.monthTxs.collectAsState()

    val income = monthTxs.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
    val expense = monthTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
    val total = balances.values.sum()
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    val catMap = remember(categories) { categories.associateBy { it.id } }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { MonthSelector(month, vm::previousMonth, vm::nextMonth) }
        item {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF5B3FD6), Color(0xFF8B5CF6))))
                    .padding(20.dp),
            ) {
                Text("Saldo totale", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Text(formatMoney(total), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Entrate", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        Text("+" + formatMoney(income), color = Color(0xFF8CF5C8), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Uscite", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        Text("-" + formatMoney(expense), color = Color(0xFFFFB4B4), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (accounts.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(accounts, key = { it.id }) { a ->
                        VCard(modifier = Modifier.width(150.dp)) {
                            EmojiBadge(a.icon, colorOf(a.colorIdx), 36)
                            Spacer(Modifier.height(8.dp))
                            Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            Text(formatMoney(balances[a.id] ?: 0L), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            val byCat = monthTxs.filter { it.type == TxType.EXPENSE }
                .groupBy { it.categoryId }
                .map { (id, list) -> id to list.sumOf { it.amountCents } }
                .sortedByDescending { it.second }
            VCard {
                Text("Spese per categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                if (byCat.isEmpty()) {
                    Text("Nessuna spesa in questo mese.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val slices = byCat.map { (id, cents) ->
                        val c = id?.let { catMap[it] }
                        Slice(c?.name ?: "Senza categoria", cents.toFloat(), colorOf(c?.colorIdx ?: 7))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(slices, Modifier.size(130.dp)) {
                            Text(formatMoney(expense), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            slices.take(5).forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${s.label} ${(s.value / expense * 100).roundToInt()}%",
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Ultimi movimenti", Modifier.weight(1f))
                TextButton(onClick = onSeeAll) { Text("Vedi tutti") }
            }
        }
        if (monthTxs.isEmpty()) {
            item { EmptyState("💸", "Nessun movimento in questo mese.\nTocca + per aggiungerne uno.") }
        }
        items(monthTxs.take(5), key = { it.id }) { tx -> TxRow(tx, accMap, catMap) { onEditTx(tx) } }
    }
}

@Composable
fun FinanceTransactions(vm: FinanceViewModel, onEditTx: (FinTx) -> Unit) {
    val month by vm.month.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    val monthTxs by vm.monthTxs.collectAsState()
    val accMap = remember(accounts) { accounts.associateBy { it.id } }
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val grouped = remember(monthTxs) { monthTxs.groupBy { it.day } }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { MonthSelector(month, vm::previousMonth, vm::nextMonth) }
        if (monthTxs.isEmpty()) {
            item { EmptyState("📭", "Nessun movimento in questo mese.") }
        }
        grouped.forEach { (day, list) ->
            item(key = "h$day") {
                val net = list.sumOf {
                    when (it.type) {
                        TxType.INCOME -> it.amountCents
                        TxType.EXPENSE -> -it.amountCents
                        else -> 0L
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(com.vyrn.tracker.data.fmtDayLong(day).replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
                    Text(
                        (if (net > 0) "+" else "") + formatMoney(net),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (net >= 0) IncomeColor else ExpenseColor,
                    )
                }
            }
            items(list, key = { it.id }) { tx -> TxRow(tx, accMap, catMap) { onEditTx(tx) } }
        }
    }
}
