package sk.krokywidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PermissionActivity : ComponentActivity() {
    private val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    private lateinit var statusText: TextView

    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(permissions)) {
                statusText.text = "Prístup ku krokom je povolený."
                refreshWidgets()
            } else {
                statusText.text = "Prístup nebol povolený. Skúste tlačidlo znova."
            }
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        layout.addView(TextView(this).apply {
            text = "Kroky Widget"
            textSize = 28f
        })
        statusText = TextView(this).apply {
            text = "Kontrolujem Health Connect…"
            textSize = 17f
            setPadding(0, 32, 0, 32)
        }
        layout.addView(statusText)
        layout.addView(Button(this).apply {
            text = "POVOLIŤ PRÍSTUP KU KROKOM"
            setOnClickListener { askForPermission() }
        })
        layout.addView(Button(this).apply {
            text = "OBNOVIŤ WIDGET"
            setOnClickListener { checkAndRefresh() }
        })
        setContentView(layout)
        checkAndRefresh()
    }

    private fun askForPermission() {
        val sdkStatus = HealthConnectClient.getSdkStatus(this)
        if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
            requestPermissions.launch(permissions)
        } else {
            statusText.text = "Health Connect nie je v telefóne dostupný alebo je vypnutý."
            Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRefresh() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
            statusText.text = "Health Connect nie je dostupný. Skontrolujte ho v Nastaveniach telefónu."
            return
        }
        lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@PermissionActivity)
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                    statusText.text = "Prístup ku krokom je povolený. Widget sa obnovuje."
                    refreshWidgets()
                } else {
                    statusText.text = "Prístup ku krokom zatiaľ nie je povolený."
                }
            } catch (error: Exception) {
                statusText.text = "Health Connect hlási chybu: " + (error.message ?: error.javaClass.simpleName)
            }
        }
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val component = ComponentName(this, StepsWidgetProvider::class.java)
        StepsWidgetProvider.update(this, manager, manager.getAppWidgetIds(component))
    }
}
