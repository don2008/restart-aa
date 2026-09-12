package sk.krokywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class StepsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, manager, ids)
    }
    companion object {
        const val PREFS = "widget_style"
        const val KEY_COLOR = "color"
        const val KEY_OPACITY = "opacity"
        private val COLORS = intArrayOf(
            Color.rgb(7, 94, 84), Color.rgb(20, 88, 180), Color.rgb(103, 58, 183),
            Color.rgb(230, 108, 25), Color.rgb(190, 45, 55), Color.rgb(25, 25, 28)
        )
        fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.steps_widget)
                applyStyle(context, views)
                val pending = PendingIntent.getActivity(context, 0,
                    Intent(context, PermissionActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_root, pending)
                try {
                    val client = HealthConnectClient.getOrCreate(context)
                    val permission = HealthPermission.getReadPermission(StepsRecord::class)
                    if (!client.permissionController.getGrantedPermissions().contains(permission)) {
                        views.setTextViewText(R.id.steps_count, "Povoliť kroky")
                        views.setTextViewText(R.id.steps_label, "Ťuknite na widget")
                    } else {
                        val zone = ZoneId.systemDefault()
                        val result = client.readRecords(ReadRecordsRequest(
                            recordType = StepsRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(
                                LocalDate.now(zone).atStartOfDay(zone).toInstant(), Instant.now())))
                        val total = result.records.sumOf { it.count }
                        views.setTextViewText(R.id.steps_count,
                            NumberFormat.getIntegerInstance(Locale.getDefault()).format(total))
                        views.setTextViewText(R.id.steps_label, "krokov dnes  •  ťuknutím obnoviť")
                    }
                } catch (error: Exception) {
                    views.setTextViewText(R.id.steps_count, "—")
                    views.setTextViewText(R.id.steps_label, "Otvorte aplikáciu a skontrolujte prístup")
                }
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }
        private fun applyStyle(context: Context, views: RemoteViews) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val index = prefs.getInt(KEY_COLOR, 0).coerceIn(COLORS.indices)
            val opacity = prefs.getInt(KEY_OPACITY, 92).coerceIn(20, 100)
            val base = COLORS[index]
            val color = Color.argb(opacity * 255 / 100, Color.red(base), Color.green(base), Color.blue(base))
            val bitmap = Bitmap.createBitmap(1000, 180, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawRoundRect(2f, 2f, 998f, 178f, 44f, 44f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
            views.setImageViewBitmap(R.id.widget_bg, bitmap)
        }
    }
}
