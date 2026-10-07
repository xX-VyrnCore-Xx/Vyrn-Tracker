package com.vyrn.tracker.ui.finance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyrn.tracker.FinanceViewModel
import com.vyrn.tracker.data.Account
import com.vyrn.tracker.data.Category
import com.vyrn.tracker.data.centsToInput
import com.vyrn.tracker.data.formatMoney
import com.vyrn.tracker.data.parseCents
import com.vyrn.tracker.ui.AccountEmojis
import com.vyrn.tracker.ui.CategoryEmojis
import com.vyrn.tracker.ui.ChipRow
import com.vyrn.tracker.ui.ColorPicker
import com.vyrn.tracker.ui.DecimalField
import com.vyrn.tracker.ui.EmojiBadge
import com.vyrn.tracker.ui.EmojiPicker
import com.vyrn.tracker.ui.FormDialog
import com.vyrn.tracker.ui.ScreenPadding
import com.vyrn.tracker.ui.SectionTitle
import com.vyrn.tracker.ui.TextInput
import com.vyrn.tracker.ui.VCard
import com.vyrn.tracker.ui.colorOf
import java.time.LocalDate

@Composable
fun FinanceManage(vm: FinanceViewModel) {
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    val balances by vm.balances.collectAsState()
    var accountEditor by remember { mutableStateOf<Account?>(null) }
    var categoryEditor by remember { mutableStateOf<Category?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importCsv(uri)
    }

    LazyColumn(contentPadding = ScreenPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("Conti") }
        item { AddButton("Nuovo conto") { accountEditor = Account(name = "") } }
        accounts.forEach { a ->
            item(key = "a${a.id}") {
                VCard(onClick = { accountEditor = a }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(a.icon, colorOf(a.colorIdx), 40)
                        Spacer(Modifier.width(12.dp))
                        Text(a.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(formatMoney(balances[a.id] ?: 0L), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { SectionTitle("Categorie di spesa") }
        item { AddButton("Nuova categoria") { categoryEditor = Category(name = "") } }
        categories.filter { !it.isIncome }.forEach { c ->
            item(key = "c${c.id}") { CategoryRow(c) { categoryEditor = c } }
        }
        item { SectionTitle("Categorie di entrata") }
        categories.filter { it.isIncome }.forEach { c ->
            item(key = "c${c.id}") { CategoryRow(c) { categoryEditor = c } }
        }

        item { SectionTitle("Dati") }
        item {
            VCard {
                Text(
                    "Esporta o importa i movimenti in formato CSV (Data, Tipo, Importo, Conto, Categoria, Nota).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportLauncher.launch("vyrn-movimenti-${LocalDate.now()}.csv") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Esporta")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Importa")
                    }
                }
            }
        }
    }

    accountEditor?.let { a ->
        var name by remember(a) { mutableStateOf(a.name) }
        var icon by remember(a) { mutableStateOf(a.icon) }
        var colorIdx by remember(a) { mutableStateOf(a.colorIdx) }
        var initial by remember(a) { mutableStateOf(if (a.initialCents != 0L) centsToInput(a.initialCents) else "") }
        FormDialog(
            title = if (a.id == 0L) "Nuovo conto" else "Modifica conto",
            onDismiss = { accountEditor = null },
            confirmEnabled = name.isNotBlank(),
            onDelete = if (a.id != 0L && accounts.size > 1) ({ vm.deleteAccount(a); accountEditor = null }) else null,
            onConfirm = {
                vm.saveAccount(a.copy(name = name.trim(), icon = icon, colorIdx = colorIdx, initialCents = parseCents(initial) ?: 0L))
                accountEditor = null
            },
        ) {
            TextInput("Nome", name, { name = it })
            EmojiPicker(AccountEmojis, icon) { icon = it }
            ColorPicker(colorIdx) { colorIdx = it }
            DecimalField("Saldo iniziale (€)", initial, { initial = it })
            if (a.id != 0L) {
                Text(
                    "Eliminando il conto verranno eliminati anche i suoi movimenti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    categoryEditor?.let { c ->
        var name by remember(c) { mutableStateOf(c.name) }
        var icon by remember(c) { mutableStateOf(c.icon) }
        var colorIdx by remember(c) { mutableStateOf(c.colorIdx) }
        var isIncome by remember(c) { mutableStateOf(c.isIncome) }
        FormDialog(
            title = if (c.id == 0L) "Nuova categoria" else "Modifica categoria",
            onDismiss = { categoryEditor = null },
            confirmEnabled = name.isNotBlank(),
            onDelete = if (c.id != 0L) ({ vm.deleteCategory(c); categoryEditor = null }) else null,
            onConfirm = {
                vm.saveCategory(c.copy(name = name.trim(), icon = icon, colorIdx = colorIdx, isIncome = isIncome))
                categoryEditor = null
            },
        ) {
            TextInput("Nome", name, { name = it })
            EmojiPicker(CategoryEmojis, icon) { icon = it }
            ColorPicker(colorIdx) { colorIdx = it }
            if (c.id == 0L) {
                ChipRow(listOf(false, true), isIncome, { if (it) "Entrata" else "Spesa" }) { isIncome = it }
            }
        }
    }
}

@Composable
private fun CategoryRow(c: Category, onClick: () -> Unit) {
    VCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(c.icon, colorOf(c.colorIdx), 36)
            Spacer(Modifier.width(12.dp))
            Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        }
    }
}

