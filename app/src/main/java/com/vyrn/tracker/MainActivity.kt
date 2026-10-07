package com.vyrn.tracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.fragment.app.FragmentActivity
import com.vyrn.tracker.lock.AppLock
import com.vyrn.tracker.ui.LockScreen
import com.vyrn.tracker.ui.CalendarScreen
import com.vyrn.tracker.ui.StatsScreen
import com.vyrn.tracker.ui.finance.FinanceScreen
import com.vyrn.tracker.ui.routine.RoutineScreen
import com.vyrn.tracker.ui.theme.ThemeSettings
import com.vyrn.tracker.update.UpdateChecker
import com.vyrn.tracker.ui.theme.VyrnTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeSettings.load(this)
        AppLock.init(this)
        enableEdgeToEdge()
        setContent {
            VyrnTheme {
                if (AppLock.locked) LockScreen() else VyrnApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Con il PIN attivo l'app non compare nelle anteprime recenti né negli screenshot.
        if (AppLock.pinSet) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        AppLock.onForeground()
    }

    override fun onStop() {
        super.onStop()
        AppLock.onBackground()
    }
}

private enum class MainTab(val label: String, val icon: ImageVector, val iconSelected: ImageVector) {
    Routine("Routine", Icons.Outlined.Checklist, Icons.Rounded.Checklist),
    Calendar("Calendario", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
    Finance("Finanza", Icons.Outlined.AccountBalanceWallet, Icons.Rounded.AccountBalanceWallet),
    Stats("Statistiche", Icons.Outlined.BarChart, Icons.Rounded.BarChart),
}

@Composable
private fun VyrnApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val ctx = LocalContext.current
    var update by remember { mutableStateOf<UpdateChecker.Info?>(null) }
    LaunchedEffect(Unit) {
        if (UpdateChecker.enabled(ctx)) {
            val info = UpdateChecker.check(ctx)
            if (info != null && info.version != UpdateChecker.dismissedVersion(ctx)) update = info
        }
    }
    update?.let { info ->
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { UpdateChecker.dismiss(ctx, info.version); update = null },
            title = { Text("Nuova versione disponibile") },
            text = { Text("È uscita la versione ${info.version} di Vyrn Tracker. Scaricala dalla pagina delle Release: si installa sopra quella attuale senza perdere i dati.") },
            confirmButton = {
                TextButton(onClick = {
                    uriHandler.openUri(info.url)
                    update = null
                }) { Text("Scarica") }
            },
            dismissButton = {
                TextButton(onClick = { UpdateChecker.dismiss(ctx, info.version); update = null }) { Text("Più tardi") }
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(if (tab == i) t.iconSelected else t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
                label = "mainTabs",
            ) { t ->
                when (MainTab.entries[t]) {
                    MainTab.Routine -> RoutineScreen()
                    MainTab.Calendar -> CalendarScreen()
                    MainTab.Finance -> FinanceScreen()
                    MainTab.Stats -> StatsScreen()
                }
            }
        }
    }
}
