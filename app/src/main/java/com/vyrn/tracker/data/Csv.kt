package com.vyrn.tracker.data

import androidx.room.withTransaction
import java.time.LocalDate

/**
 * Formato CSV dei movimenti: Data,Tipo,Importo,Conto,Categoria,Nota
 * Data in formato AAAA-MM-GG, importo con il punto come separatore decimale.
 * Per i trasferimenti la colonna "Categoria" contiene il conto di destinazione.
 */
object Csv {
    const val HEADER = "Data,Tipo,Importo,Conto,Categoria,Nota"

    private fun esc(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun export(txs: List<FinTx>, accounts: List<Account>, categories: List<Category>): String {
        val accName = accounts.associate { it.id to it.name }
        val catName = categories.associate { it.id to it.name }
        val sb = StringBuilder(HEADER).append('\n')
        for (t in txs.sortedWith(compareBy<FinTx> { it.day }.thenBy { it.id })) {
            val type = when (t.type) {
                TxType.INCOME -> "Entrata"
                TxType.EXPENSE -> "Uscita"
                else -> "Trasferimento"
            }
            val third = if (t.type == TxType.TRANSFER) accName[t.toAccountId] ?: "" else catName[t.categoryId] ?: ""
            sb.append(LocalDate.ofEpochDay(t.day)).append(',')
                .append(type).append(',')
                .append(String.format(java.util.Locale.US, "%.2f", t.amountCents / 100.0)).append(',')
                .append(esc(accName[t.accountId] ?: "")).append(',')
                .append(esc(third)).append(',')
                .append(esc(t.note)).append('\n')
        }
        return sb.toString()
    }

    fun parseLine(line: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    out.add(cur.toString())
                    cur.clear()
                }
                else -> cur.append(c)
            }
            i++
        }
        out.add(cur.toString())
        return out
    }

    /** Divide il testo in record rispettando i ritorni a capo dentro le virgolette. */
    fun splitRecords(text: String): List<String> {
        val records = ArrayList<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (c in text) {
            if (c == '"') inQuotes = !inQuotes
            if ((c == '\n' || c == '\r') && !inQuotes) {
                if (cur.isNotEmpty()) records.add(cur.toString())
                cur.clear()
            } else cur.append(c)
        }
        if (cur.isNotEmpty()) records.add(cur.toString())
        return records
    }

    /** Importa i movimenti creando conti/categorie mancanti. Restituisce il numero di righe importate. */
    suspend fun importText(text: String, db: AppDatabase): Int = db.withTransaction {
        val dao = db.financeDao()
        val accounts = dao.getAccounts().associateBy { it.name.lowercase() }.toMutableMap()
        val categories = dao.getCategories().associateBy { (if (it.isIncome) "i:" else "e:") + it.name.lowercase() }.toMutableMap()

        suspend fun account(name: String): Long {
            val key = name.trim().ifBlank { "Importato" }
            accounts[key.lowercase()]?.let { return it.id }
            val id = dao.insertAccount(Account(name = key))
            accounts[key.lowercase()] = Account(id = id, name = key)
            return id
        }

        suspend fun category(name: String, income: Boolean): Long? {
            val key = name.trim()
            if (key.isEmpty()) return null
            val k = (if (income) "i:" else "e:") + key.lowercase()
            categories[k]?.let { return it.id }
            val id = dao.insertCategory(Category(name = key, isIncome = income, colorIdx = categories.size))
            categories[k] = Category(id = id, name = key, isIncome = income)
            return id
        }

        val list = ArrayList<FinTx>()
        val records = splitRecords(text)
        for ((index, rec) in records.withIndex()) {
            if (index == 0 && rec.startsWith("Data,")) continue
            val f = parseLine(rec)
            if (f.size < 4) continue
            val day = runCatching { LocalDate.parse(f[0].trim()).toEpochDay() }.getOrNull() ?: continue
            val cents = parseCents(f[2]) ?: continue
            if (cents <= 0) continue
            val third = f.getOrElse(4) { "" }
            val note = f.getOrElse(5) { "" }
            val accId = account(f[3])
            when (f[1].trim().lowercase()) {
                "entrata" -> list.add(FinTx(accountId = accId, categoryId = category(third, true), type = TxType.INCOME, amountCents = cents, day = day, note = note))
                "uscita" -> list.add(FinTx(accountId = accId, categoryId = category(third, false), type = TxType.EXPENSE, amountCents = cents, day = day, note = note))
                "trasferimento" -> {
                    val to = account(third)
                    if (to != accId) list.add(FinTx(accountId = accId, type = TxType.TRANSFER, amountCents = cents, toAccountId = to, day = day, note = note))
                }
            }
        }
        dao.insertAllTx(list)
        list.size
    }
}
