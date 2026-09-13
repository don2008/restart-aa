package sk.krokywidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.widget.RemoteViews
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.time.*
import java.util.*
import java.util.concurrent.TimeUnit

class StepsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) =
        update(context, manager, ids)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedule(context)
        UnlockUpdateService.start(context)
    }

    companion object {
        const val PREFS = "widget_style"
        const val KEY_COLOR = "color"
        const val KEY_OPACITY = "opacity"
        const val KEY_GOAL = "daily_goal"
        const val KEY_PROGRESS_COLOR = "progress_color"
        const val KEY_SHOE_COLOR = "shoe_color"
        private const val WORK_NAME = "steps-widget-refresh"
        private val COLORS = intArrayOf(
            Color.rgb(7,94,84), Color.rgb(20,88,180), Color.rgb(103,58,183),
            Color.rgb(230,108,25), Color.rgb(190,45,55), Color.rgb(25,25,28))
        private val SHOE_IMAGES = intArrayOf(
            R.drawable.shoe_red, R.drawable.shoe_blue, R.drawable.shoe_green,
            R.drawable.shoe_orange, R.drawable.shoe_purple, R.drawable.shoe_silver)
        private val PROGRESS_COLORS = intArrayOf(
            Color.rgb(244,67,54), Color.rgb(76,175,80), Color.rgb(33,150,243),
            Color.rgb(255,152,0), Color.rgb(255,214,0), Color.rgb(156,39,176),
            Color.WHITE)

        fun schedule(context: Context) {
            val work = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, work)
        }

        fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch { updateNow(context, manager, ids) }
        }

        suspend fun updateNow(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.steps_widget)
            applyStyle(context, views)
            views.setProgressBar(R.id.goal_progress, 100, 0, false)
            views.setTextViewText(R.id.goal_status, "")
            views.setTextViewText(R.id.update_time, "")
            val pending = PendingIntent.getActivity(context, 0,
                Intent(context, PermissionActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val required = setOf(
                    HealthPermission.getReadPermission(StepsRecord::class),
                    HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)
                if (!client.permissionController.getGrantedPermissions().containsAll(required)) {
                    views.setTextViewText(R.id.steps_count, "Povoliť kroky")
                    views.setTextViewText(R.id.steps_label, "Povoľte aj prístup na pozadí")
                } else {
                    val zone = ZoneId.systemDefault()
                    val result = client.readRecords(ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(
                            LocalDate.now(zone).atStartOfDay(zone).toInstant(), Instant.now())))
                    val total = result.records.sumOf { it.count }
                    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    val goal = prefs.getInt(KEY_GOAL, 10000).coerceAtLeast(1)
                    val percent = ((total * 100L) / goal).coerceIn(0L, 100L).toInt()
                    views.setTextViewText(R.id.steps_count,
                        NumberFormat.getIntegerInstance(Locale.getDefault()).format(total))
                    val goalText = NumberFormat.getIntegerInstance(Locale.getDefault()).format(goal)
                    val updated = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        .format(LocalTime.now())
                    views.setTextViewText(R.id.steps_label, "krokov dnes")
                    views.setTextViewText(R.id.goal_status, "cieľ $goalText  •  $percent %")
                    views.setTextViewText(R.id.update_time, updated)
                    views.setProgressBar(R.id.goal_progress, 100, percent, false)
                }
            } catch (error: Exception) {
                views.setTextViewText(R.id.steps_count, "—")
                views.setTextViewText(R.id.steps_label, "Otvorte aplikáciu a skontrolujte prístup")
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun applyStyle(context: Context, views: RemoteViews) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val base = COLORS[prefs.getInt(KEY_COLOR,0).coerceIn(COLORS.indices)]
            val opacity = prefs.getInt(KEY_OPACITY,92).coerceIn(20,100)
            val shoeImage = SHOE_IMAGES[
                prefs.getInt(KEY_SHOE_COLOR, 0).coerceIn(SHOE_IMAGES.indices)]
            views.setImageViewResource(R.id.shoe_icon, shoeImage)
            val progressColor = PROGRESS_COLORS[
                prefs.getInt(KEY_PROGRESS_COLOR, 0).coerceIn(PROGRESS_COLORS.indices)]
            views.setColorStateList(
                R.id.goal_progress, "setProgressTintList", ColorStateList.valueOf(progressColor))
            val color = Color.argb(opacity*255/100, Color.red(base), Color.green(base), Color.blue(base))
            val bitmap = Bitmap.createBitmap(1000,180,Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawRoundRect(2f,2f,998f,178f,44f,44f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=color })
            views.setImageViewBitmap(R.id.widget_bg, bitmap)
        }
    }
}
