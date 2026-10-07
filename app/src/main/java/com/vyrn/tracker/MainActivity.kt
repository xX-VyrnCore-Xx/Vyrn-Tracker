package com.vyrn.tracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vyrn.tracker.ui.CalendarScreen
import com.vyrn.tracker.ui.StatsScreen
import com.vyrn.tracker.ui.finance.FinanceScreen
import com.vyrn.tracker.ui.routine.RoutineScreen
import com.vyrn.tracker.ui.theme.VyrnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VyrnTheme {
                VyrnApp()
            }
        }
    }
}

private enum class MainTab(val label: String, val icon: ImageVector) {
    Routine("Routine", Icons.Rounded.Checklist),
    Calendar("Calendario", Icons.Rounded.CalendarMonth),
    Finance("Finanza", Icons.Rounded.AccountBalanceWallet),
    Stats("Statistiche", Icons.Rounded.BarChart),
}

@Composable
private fun VyrnApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (MainTab.entries[tab]) {
                MainTab.Routine -> RoutineScreen()
                MainTab.Calendar -> CalendarScreen()
                MainTab.Finance -> FinanceScreen()
                MainTab.Stats -> StatsScreen()
            }
        }
    }
}
