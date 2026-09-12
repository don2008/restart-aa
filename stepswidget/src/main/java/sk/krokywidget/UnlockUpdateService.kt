package sk.krokywidget

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class UnlockUpdateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_USER_PRESENT) return
            refresh()
            scope.launch {
                delay(10_000)
                refreshNow()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(
            unlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            Context.RECEIVER_NOT_EXPORTED
        )
        refresh()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refresh()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun refresh() {
        scope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(applicationContext, StepsWidgetProvider::class.java)
        )
        StepsWidgetProvider.updateNow(applicationContext, manager, ids)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Automatická aktualizácia krokov",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Aktualizuje widget po odomknutí telefónu"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_steps)
            .setContentTitle("Kroky Widget")
            .setContentText("Automatická aktualizácia pri odomknutí je aktívna")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    companion object {
        private const val CHANNEL_ID = "unlock_updates"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    Intent(context, UnlockUpdateService::class.java)
                )
            }
        }
    }
}
