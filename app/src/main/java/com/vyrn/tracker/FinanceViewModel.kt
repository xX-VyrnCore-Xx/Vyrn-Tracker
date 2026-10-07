package com.vyrn.tracker

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vyrn.tracker.data.Account
import com.vyrn.tracker.data.AppDatabase
import com.vyrn.tracker.data.Budget
import com.vyrn.tracker.data.Category
import com.vyrn.tracker.data.Csv
import com.vyrn.tracker.data.Debt
import com.vyrn.tracker.data.FinTx
import com.vyrn.tracker.data.Goal
import com.vyrn.tracker.data.Recurring
import com.vyrn.tracker.data.TxType
import com.vyrn.tracker.data.computeBalances
import com.vyrn.tracker.data.formatMoney
import com.vyrn.tracker.data.monthRange
import com.vyrn.tracker.data.processRecurring
import com.vyrn.tracker.notify.Reminders
import com.vyrn.tracker.widget.refreshWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val db = AppDatabase.get(ctx)
    private val dao = db.financeDao()

    private fun <T> Flow<T>.state(init: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    val accounts: StateFlow<List<Account>> = dao.observeAccounts().state(emptyList())
    val categories: StateFlow<List<Category>> = dao.observeCategories().state(emptyList())
    val txs: StateFlow<List<FinTx>> = dao.observeTx().state(emptyList())
    val budgets: StateFlow<List<Budget>> = dao.observeBudgets().state(emptyList())
    val goals: StateFlow<List<Goal>> = dao.observeGoals().state(emptyList())
    val recurring: StateFlow<List<Recurring>> = dao.observeRecurring().state(emptyList())
    val debts: StateFlow<List<Debt>> = dao.observeDebts().state(emptyList())

    /** Mese mostrato nelle schermate finanza e statistiche. */
    val month = MutableStateFlow(YearMonth.now())

    val balances: StateFlow<Map<Long, Long>> =
        combine(accounts, txs) { a, t -> computeBalances(a, t) }.state(emptyMap())

    val monthTxs: StateFlow<List<FinTx>> =
        combine(txs, month) { t, m -> val r = monthRange(m); t.filter { it.day in r } }.state(emptyList())

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
            refreshWidget(ctx)
        }
    }

    private fun toast(msg: String) {
        viewModelScope.launch(Dispatchers.Main) { Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
    }

    // ---------- Movimenti ----------

    fun saveTx(tx: FinTx) = io {
        if (tx.id == 0L) dao.insertTx(tx) else dao.updateTx(tx)
        if (tx.type == TxType.EXPENSE) checkBudget(tx)
    }

    fun deleteTx(tx: FinTx) = io { dao.deleteTx(tx.id) }

    private suspend fun checkBudget(tx: FinTx) {
        val catId = tx.categoryId ?: return
        val budget = dao.getBudgets().firstOrNull { it.categoryId == catId } ?: return
        val ym = YearMonth.from(java.time.LocalDate.ofEpochDay(tx.day))
        val range = monthRange(ym)
        val spent = dao.getAllTx()
            .filter { it.type == TxType.EXPENSE && it.categoryId == catId && it.day in range }
            .sumOf { it.amountCents }
        if (spent > budget.limitCents) {
            val name = dao.getCategories().firstOrNull { it.id == catId }?.name ?: "categoria"
            Reminders.notify(
                ctx, Reminders.KIND_BUDGET * 100_000 + (catId % 100_000).toInt(),
                "Budget superato",
                "$name: ${formatMoney(spent)} su ${formatMoney(budget.limitCents)}",
            )
        }
    }

    // ---------- Conti e categorie ----------

    fun saveAccount(a: Account) = io { if (a.id == 0L) dao.insertAccount(a) else dao.updateAccount(a) }

    fun deleteAccount(a: Account) = io {
        dao.deleteTxForAccount(a.id)
        dao.deleteRecurringForAccount(a.id)
        dao.deleteAccount(a.id)
    }

    fun saveCategory(c: Category) = io { if (c.id == 0L) dao.insertCategory(c) else dao.updateCategory(c) }

    fun deleteCategory(c: Category) = io {
        dao.clearCategoryInTx(c.id)
        dao.clearCategoryInRecurring(c.id)
        dao.deleteBudget(c.id)
        dao.deleteCategory(c.id)
    }

    // ---------- Budget ----------

    fun saveBudget(categoryId: Long, limitCents: Long) = io { dao.upsertBudget(Budget(categoryId, limitCents)) }

    fun deleteBudget(categoryId: Long) = io { dao.deleteBudget(categoryId) }

    // ---------- Obiettivi ----------

    fun saveGoal(g: Goal) = io { if (g.id == 0L) dao.insertGoal(g) else dao.updateGoal(g) }

    fun deleteGoal(g: Goal) = io { dao.deleteGoal(g.id) }

    fun changeGoalFunds(g: Goal, deltaCents: Long) =
        io { dao.updateGoal(g.copy(savedCents = (g.savedCents + deltaCents).coerceAtLeast(0L))) }

    // ---------- Ricorrenti ----------

    fun saveRecurring(r: Recurring) = io {
        if (r.id == 0L) dao.insertRecurring(r) else dao.updateRecurring(r)
        processRecurring(db)
    }

    fun deleteRecurring(r: Recurring) = io { dao.deleteRecurring(r.id) }

    fun setRecurringActive(r: Recurring, active: Boolean) = io {
        dao.updateRecurring(r.copy(active = active))
        if (active) processRecurring(db)
    }

    // ---------- Debiti ----------

    fun saveDebt(d: Debt) = io { if (d.id == 0L) dao.insertDebt(d) else dao.updateDebt(d) }

    fun deleteDebt(d: Debt) = io { dao.deleteDebt(d.id) }

    // ---------- CSV ----------

    fun exportCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = Csv.export(dao.getAllTx(), dao.getAccounts(), dao.getCategories())
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                toast("Esportazione completata")
            } catch (e: Exception) {
                toast("Errore durante l'esportazione")
            }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                val n = Csv.importText(text.removePrefix("﻿"), db)
                refreshWidget(ctx)
                toast("Importati $n movimenti")
            } catch (e: Exception) {
                toast("Errore durante l'importazione")
            }
        }
    }
}
