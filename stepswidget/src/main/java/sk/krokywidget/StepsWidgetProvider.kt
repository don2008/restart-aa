package sk.krokywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.text.NumberFormat
import java.util.Locale

class StepsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, manager, ids)
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val views = RemoteViews(context.packageName, R.layout.steps_widget)
                val open = Intent(context, PermissionActivity::class.java)
                val pending = PendingIntent.getActivity(
                    context, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pending)

                try {
                    val client = HealthConnectClient.getOrCreate(context)
                    val permission = HealthPermission.getReadPermission(StepsRecord::class)
                    val granted = client.permissionController.getGrantedPermissions()
                    if (!granted.contains(permission)) {
                        views.setTextViewText(R.id.steps_count, "Povoliť kroky")
                        views.setTextViewText(R.id.steps_label, "Ťuknite na widget")
                    } else {
                        val zone = ZoneId.systemDefault()
                        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
                        val end = Instant.now()
                        val result = client.readRecords(
                            ReadRecordsRequest(
                                recordType = StepsRecord::class,
                                timeRangeFilter = TimeRangeFilter.between(start, end)
                            )
                        )
                        val total = result.records.sumOf { it.count }
                        views.setTextViewText(
                            R.id.steps_count,
                            NumberFormat.getIntegerInstance(Locale.getDefault()).format(total)
                        )
                        views.setTextViewText(R.id.steps_label, "krokov dnes  •  ťuknutím obnoviť")
                    }
                } catch (error: Exception) {
                    views.setTextViewText(R.id.steps_count, "—")
                    views.setTextViewText(R.id.steps_label, "Health Connect nie je dostupný")
                }

                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }
    }
}
