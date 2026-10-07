package com.vyrn.tracker.ui.routine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vyrn.tracker.RoutineViewModel
import com.vyrn.tracker.ui.ScreenScaffold

@Composable
fun RoutineScreen(vm: RoutineViewModel = viewModel()) {
    var sub by rememberSaveable { mutableIntStateOf(0) }
    ScreenScaffold("Routine") {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = sub) {
                listOf("Abitudini", "Routine", "Task", "Focus").forEachIndexed { i, label ->
                    Tab(selected = sub == i, onClick = { sub = i }, text = { Text(label) })
                }
            }
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = sub,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 20 }) togetherWith fadeOut(tween(120))
                    },
                    label = "routineTabs",
                ) { s ->
                    when (s) {
                        0 -> HabitsTab(vm)
                        1 -> RoutinesTab(vm)
                        2 -> TasksTab(vm)
                        else -> FocusTab()
                    }
                }
            }
        }
    }
}
