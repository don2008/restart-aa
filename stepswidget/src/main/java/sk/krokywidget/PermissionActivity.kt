package sk.krokywidget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PermissionActivity : ComponentActivity() {
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
    )
    private lateinit var statusText: TextView
    private lateinit var colorSpinner: Spinner
    private lateinit var opacitySeek: SeekBar
    private lateinit var opacityText: TextView
    private lateinit var goalEdit: EditText
    private lateinit var progressColorSpinner: Spinner
    private val colorNames = arrayOf(
        "Smaragdová", "Modrá", "Fialová", "Oranžová", "Červená", "Čierna")
    private val progressColorNames = arrayOf(
        "Červená", "Zelená", "Modrá", "Oranžová", "Žltá", "Fialová", "Biela")

    private val requestPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(permissions)) {
                statusText.text = "Prístup ku krokom je povolený."
                refreshWidgets()
            } else statusText.text = "Prístup nebol povolený. Skúste tlačidlo znova."
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val prefs = getSharedPreferences(StepsWidgetProvider.PREFS, MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 56, 48, 48)
        }
        val scroll = ScrollView(this).apply { addView(layout) }

        layout.addView(TextView(this).apply { text = "Kroky Widget"; textSize = 28f })
        statusText = TextView(this).apply {
            text = "Kontrolujem Health Connect…"; textSize = 16f; setPadding(0, 24, 0, 20)
        }
        layout.addView(statusText)
        layout.addView(Button(this).apply {
            text = "POVOLIŤ PRÍSTUP KU KROKOM"; setOnClickListener { askForPermission() }
        })
        layout.addView(Button(this).apply {
            text = "POVOLIŤ AUTOMATICKÚ AKTUALIZÁCIU"
            setOnClickListener { requestReliableBackgroundRun() }
        })

        layout.addView(TextView(this).apply {
            text = "Denný cieľ počtu krokov"; textSize = 18f; setPadding(0, 30, 0, 8)
        })
        goalEdit = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt(StepsWidgetProvider.KEY_GOAL, 10000).toString())
            hint = "napríklad 10000"
        }
        layout.addView(goalEdit)

        layout.addView(TextView(this).apply {
            text = "Farba ukazovateľa cieľa"; textSize = 18f; setPadding(0, 24, 0, 8)
        })
        progressColorSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@PermissionActivity,
                android.R.layout.simple_spinner_dropdown_item,
                progressColorNames)
            setSelection(prefs.getInt(StepsWidgetProvider.KEY_PROGRESS_COLOR, 0))
        }
        layout.addView(progressColorSpinner)

        layout.addView(TextView(this).apply {
            text = "Farba pozadia"; textSize = 18f; setPadding(0, 24, 0, 8)
        })
        colorSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@PermissionActivity,
                android.R.layout.simple_spinner_dropdown_item,
                colorNames)
            setSelection(prefs.getInt(StepsWidgetProvider.KEY_COLOR, 0))
        }
        layout.addView(colorSpinner)

        opacityText = TextView(this).apply { textSize = 18f; setPadding(0, 24, 0, 4) }
        layout.addView(opacityText)
        opacitySeek = SeekBar(this).apply {
            max = 80
            progress = prefs.getInt(StepsWidgetProvider.KEY_OPACITY, 92) - 20
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    opacityText.text = "Priehľadnosť pozadia: " + (value + 20) + " %"
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        opacityText.text = "Priehľadnosť pozadia: " + (opacitySeek.progress + 20) + " %"
        layout.addView(opacitySeek)

        layout.addView(Button(this).apply {
            text = "ULOŽIŤ NASTAVENIA A OBNOVIŤ"
            setOnClickListener {
                val goal = goalEdit.text.toString().toIntOrNull()
                if (goal == null || goal !in 100..100000) {
                    Toast.makeText(
                        this@PermissionActivity,
                        "Zadajte cieľ od 100 do 100 000 krokov.",
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                prefs.edit()
                    .putInt(StepsWidgetProvider.KEY_GOAL, goal)
                    .putInt(
                        StepsWidgetProvider.KEY_PROGRESS_COLOR,
                        progressColorSpinner.selectedItemPosition)
                    .putInt(StepsWidgetProvider.KEY_COLOR, colorSpinner.selectedItemPosition)
                    .putInt(StepsWidgetProvider.KEY_OPACITY, opacitySeek.progress + 20)
                    .apply()
                Toast.makeText(
                    this@PermissionActivity,
                    "Nastavenia boli uložené.",
                    Toast.LENGTH_SHORT).show()
                refreshWidgets()
            }
        })

        setContentView(scroll)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        StepsWidgetProvider.schedule(this)
        UnlockUpdateService.start(this)
        checkAndRefresh()
    }

    override fun onResume() {
        super.onResume()
        UnlockUpdateService.start(this)
    }

    private fun requestReliableBackgroundRun() {
        val power = getSystemService(PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(
                this, "Automatická aktualizácia je povolená.", Toast.LENGTH_SHORT).show()
            UnlockUpdateService.start(this)
            return
        }
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    private fun askForPermission() {
        if (HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE)
            requestPermissions.launch(permissions)
        else {
            statusText.text = "Health Connect nie je v telefóne dostupný alebo je vypnutý."
            Toast.makeText(this, statusText.text, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRefresh() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
            statusText.text =
                "Health Connect nie je dostupný. Skontrolujte ho v Nastaveniach telefónu."
            return
        }
        lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@PermissionActivity)
                if (client.permissionController.getGrantedPermissions().containsAll(permissions)) {
                    statusText.text = "Prístup ku krokom je povolený."
                    refreshWidgets()
                } else statusText.text = "Prístup ku krokom zatiaľ nie je povolený."
            } catch (error: Exception) {
                statusText.text =
                    "Health Connect hlási chybu: " +
                    (error.message ?: error.javaClass.simpleName)
            }
        }
    }

    private fun refreshWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val component = ComponentName(this, StepsWidgetProvider::class.java)
        StepsWidgetProvider.update(this, manager, manager.getAppWidgetIds(component))
    }
}
