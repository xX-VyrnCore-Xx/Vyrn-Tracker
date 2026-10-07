package com.vyrn.tracker

import com.vyrn.tracker.data.Account
import com.vyrn.tracker.data.Category
import com.vyrn.tracker.data.Csv
import com.vyrn.tracker.data.FinTx
import com.vyrn.tracker.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvTest {
    @Test
    fun parseLineHandlesQuotesAndCommas() {
        assertEquals(listOf("a", "b,c", "d"), Csv.parseLine("a,\"b,c\",d"))
        assertEquals(listOf("he said \"hi\"", "x"), Csv.parseLine("\"he said \"\"hi\"\"\",x"))
    }

    @Test
    fun splitRecordsKeepsNewlinesInsideQuotes() {
        val records = Csv.splitRecords("a,b\n\"x\ny\",z\n")
        assertEquals(2, records.size)
        assertEquals("\"x\ny\",z", records[1])
    }

    @Test
    fun exportWritesHeaderAndRows() {
        val tx = FinTx(
            accountId = 1, categoryId = 1, type = TxType.EXPENSE, amountCents = 1250,
            day = LocalDate.of(2026, 10, 7).toEpochDay(), note = "nota, con virgola",
        )
        val text = Csv.export(listOf(tx), listOf(Account(id = 1, name = "Conto")), listOf(Category(id = 1, name = "Spesa")))
        val lines = text.trim().split("\n")
        assertEquals(Csv.HEADER, lines[0])
        assertEquals("2026-10-07,Uscita,12.50,Conto,Spesa,\"nota, con virgola\"", lines[1])
    }
}
