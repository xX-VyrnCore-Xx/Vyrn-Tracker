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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vyrn.tracker.MainActivity
import com.vyrn.tracker.data.AppDatabase
import com.vyrn.tracker.data.HabitLog
import com.vyrn.tracker.data.HabitLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

internal data class WidgetHabit(val id: Long, val icon: String, val name: String, val done: Boolean)

private val HabitIdKey = ActionParameters.Key<Long>("habitId")

/** Widget grande: elenco delle abitudini di oggi, toccando una riga si segna come fatta. */
class HabitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = withContext(Dispatchers.IO) { load(context) }
        provideContent { HabitsContent(items) }
    }

    private suspend fun load(context: Context): List<WidgetHabit> {
        val db = AppDatabase.get(context)
        val today = LocalDate.now()
        val logs = db.habitDao().getLogsForDay(today.toEpochDay()).associateBy { it.habitId }
        return db.habitDao().getHabits()
            .filter { HabitLogic.isScheduled(it, today) }
            .map { WidgetHabit(it.id, it.icon, it.name, HabitLogic.isDone(it, logs[it.id])) }
    }
}

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsWidget()
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitIdKey] ?: return
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.get(context).habitDao()
            val habit = dao.getHabits().firstOrNull { it.id == id } ?: return@withContext
            val day = LocalDate.now().toEpochDay()
            val log = dao.getLog(id, day)
            val done = HabitLogic.isDone(habit, log)
            val value = if (habit.kind == 2) (if (done) 1.0 else 0.0) else (if (done) 0.0 else habit.target)
            val note = log?.note.orEmpty()
            if (value <= 0.0 && note.isBlank()) dao.deleteLog(id, day)
            else dao.upsertLog(HabitLog(id, day, value, note))
        }
        HabitsWidget().updateAll(context)
        TrackerWidget().updateAll(context)
    }
}

@Composable
internal fun HabitsContent(items: List<WidgetHabit>) {
    val context = LocalContext.current
    val textColor = ColorProvider(Color(0xFFF1ECFA))
    val accent = ColorProvider(Color(0xFFB9A8FF))
    val done = items.count { it.done }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF1E1A27)))
            .cornerRadius(22.dp)
            .padding(14.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Abitudini di oggi",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
            Text("$done/${items.size}", style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(GlanceModifier.height(6.dp))
        if (items.isEmpty()) {
            Text("Nessuna abitudine prevista", style = TextStyle(color = textColor, fontSize = 13.sp))
        }
        items.take(6).forEach { h ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitIdKey to h.id))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (h.done) "✅" else "⬜", style = TextStyle(fontSize = 15.sp))
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    "${h.icon} ${h.name}",
                    style = TextStyle(
                        color = if (h.done) accent else textColor,
                        fontSize = 14.sp,
                        fontWeight = if (h.done) FontWeight.Normal else FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
