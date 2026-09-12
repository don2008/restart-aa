package sk.krokywidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PermissionActivity : ComponentActivity() {
    private val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            refreshWidgets()
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
        layout.addView(TextView(this).apply {
            text = "Povoľte čítanie krokov z Health Connect. Potom pridajte widget Kroky Widget na domovskú obrazovku."
            textSize = 17f
            setPadding(0, 32, 0, 32)
        })
        layout.addView(Button(this).apply {
            text = "POVOLIŤ PRÍSTUP KU KROKOM"
            setOnClickListener { askForPermission() }
        })
        layout.addView(Button(this).apply {
            text = "OBNOVIŤ WIDGET"
            setOnClickListener { refreshWidgets() }
        })
        setContentView(layout)

        lifecycleScope.launch {
            val client = HealthConnectClient.getOrCreate(this@PermissionActivity)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(permissions)) requestPermissions.launch(permissions)
            else refreshWidgets()
        }
    }

    private fun askForPermission() {
        requestPermissions.launch(permissions)
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val component = ComponentName(this, StepsWidgetProvider::class.java)
        StepsWidgetProvider.update(this, manager, manager.getAppWidgetIds(component))
    }
}
