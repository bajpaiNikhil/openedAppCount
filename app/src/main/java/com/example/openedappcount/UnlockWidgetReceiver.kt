package com.example.openedappcount

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UnlockWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        scheduleAlarm(context)
    }

    override fun onDisabled(context: Context) {
        cancelAlarm(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { update(context, appWidgetManager, it) }
        scheduleAlarm(context)
        // Re-start the monitor service after device reboot — the system fires
        // APPWIDGET_UPDATE a few seconds after boot, before MainActivity runs.
        UnlockMonitorService.start(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TICK, Intent.ACTION_USER_PRESENT -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val manager = AppWidgetManager.getInstance(context)
                        val ids = manager.getAppWidgetIds(
                            ComponentName(context, UnlockWidgetReceiver::class.java)
                        )
                        ids.forEach { update(context, manager, it) }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION_TICK = "com.example.openedappcount.WIDGET_TICK"

        fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val count = try {
                UsageStatsRepository(context).getUnlockCount()
            } catch (e: SecurityException) {
                0
            }
            val timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

            val views = RemoteViews(context.packageName, R.layout.widget_unlock)
            views.setTextViewText(R.id.count_text, count.toString())
            views.setTextViewText(R.id.timestamp_text, "Updated $timestamp")

            val tapIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, tapIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun scheduleAlarm(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                alarmPendingIntent(context)
            )
        }

        private fun cancelAlarm(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(alarmPendingIntent(context))
        }

        private fun alarmPendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, UnlockWidgetReceiver::class.java).apply { action = ACTION_TICK },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
