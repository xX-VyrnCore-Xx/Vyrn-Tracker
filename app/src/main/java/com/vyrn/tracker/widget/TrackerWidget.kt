package com.vyrn.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vyrn.tracker.MainActivity
import com.vyrn.tracker.data.AppDatabase
import com.vyrn.tracker.data.HabitLogic
import com.vyrn.tracker.data.computeBalances
import com.vyrn.tracker.data.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal data class WidgetData(val done: Int, val total: Int, val balanceCents: Long)

class TrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { load(context) }
        provideContent { WidgetContent(data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val db = AppDatabase.get(context)
        val today = LocalDate.now()
        val habits = db.habitDao().getHabits().filter { HabitLogic.isScheduled(it, today) }
        val logs = db.habitDao().getLogsForDay(today.toEpochDay()).associateBy { it.habitId }
        val done = habits.count { HabitLogic.isDone(it, logs[it.id]) }
        val balances = computeBalances(db.financeDao().getAccounts(), db.financeDao().getAllTx())
        return WidgetData(done, habits.size, balances.values.sum())
    }
}

class TrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TrackerWidget()
}

suspend fun refreshWidget(context: Context) {
    try {
        TrackerWidget().updateAll(context)
    } catch (_: Exception) {
        // Il widget è opzionale: ignora errori di aggiornamento.
    }
}

@Composable
internal fun WidgetContent(d: WidgetData) {
    val context = LocalContext.current
    val textColor = ColorProvider(Color(0xFFF1ECFA))
    val accent = ColorProvider(Color(0xFFB9A8FF))
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1E1A27)))
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Vyrn Tracker", style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxSize()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("${d.done}/${d.total}", style = TextStyle(color = textColor, fontSize = 26.sp, fontWeight = FontWeight.Bold))
                Text("abitudini oggi", style = TextStyle(color = accent, fontSize = 12.sp))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(formatMoney(d.balanceCents), style = TextStyle(color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Text("saldo totale", style = TextStyle(color = accent, fontSize = 12.sp))
            }
        }
    }
}
