package com.example.openedappcount

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.openedappcount.ui.theme.OpenedAppCountTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val wellbeingViewModel: WellbeingViewModel by viewModels()

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!UsagePermissionHelper.hasUsageStatsPermission(this)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        UnlockWidgetReceiver.scheduleAlarm(this)
        UnlockMonitorService.start(this)
        setContent {
            OpenedAppCountTheme {
                WellbeingScreen(vm = wellbeingViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wellbeingViewModel.refresh(showLoading = false)
        widgetScope.launch {
            val manager = AppWidgetManager.getInstance(this@MainActivity)
            val ids = manager.getAppWidgetIds(
                ComponentName(this@MainActivity, UnlockWidgetReceiver::class.java)
            )
            ids.forEach { UnlockWidgetReceiver.update(this@MainActivity, manager, it) }
        }
    }
}
