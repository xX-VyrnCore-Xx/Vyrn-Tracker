package com.vyrn.tracker.ui.finance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vyrn.tracker.FinanceViewModel
import com.vyrn.tracker.data.FinTx
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.ui.ScreenScaffold
import java.time.LocalDate

private val FINANCE_TABS = listOf("Panoramica", "Movimenti", "Budget", "Obiettivi", "Ricorrenti", "Debiti", "Gestione")

@Composable
fun FinanceScreen(vm: FinanceViewModel = viewModel()) {
    var sub by rememberSaveable { mutableIntStateOf(0) }
    val accounts by vm.accounts.collectAsState()
    val categories by vm.categories.collectAsState()
    var txEditor by remember { mutableStateOf<FinTx?>(null) }

    ScreenScaffold("Finanza") {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ScrollableTabRow(selectedTabIndex = sub, edgePadding = 8.dp) {
                    FINANCE_TABS.forEachIndexed { i, label ->
                        Tab(selected = sub == i, onClick = { sub = i }, text = { Text(label) })
                    }
                }
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = sub,
                        transitionSpec = {
                            (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 20 }) togetherWith fadeOut(tween(120))
                        },
                        label = "financeTabs",
                    ) { s ->
                        when (s) {
                            0 -> FinanceOverview(vm, onEditTx = { txEditor = it }, onSeeAll = { sub = 1 })
                            1 -> FinanceTransactions(vm, onEditTx = { txEditor = it })
                            2 -> FinanceBudgets(vm)
                            3 -> FinanceGoals(vm)
                            4 -> FinanceRecurring(vm)
                            5 -> FinanceDebts(vm)
                            else -> FinanceManage(vm)
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = sub <= 1,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        txEditor = FinTx(
                            accountId = accounts.firstOrNull()?.id ?: 0L,
                            type = TxType.EXPENSE,
                            amountCents = 0,
                            day = LocalDate.now().toEpochDay(),
                        )
                    },
                ) { Icon(Icons.Rounded.Add, contentDescription = "Nuovo movimento") }
            }
        }
    }

    txEditor?.let { tx ->
        TxEditor(
            initial = tx,
            accounts = accounts,
            categories = categories,
            onDismiss = { txEditor = null },
            onSave = { vm.saveTx(it); txEditor = null },
            onDelete = if (tx.id != 0L) ({ vm.deleteTx(tx); txEditor = null }) else null,
        )
    }
}
