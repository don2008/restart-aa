package sk.krokywidget

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class RationaleActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(TextView(this).apply {
            text = "Kroky Widget číta iba dnešný počet krokov z Health Connect, aby ho zobrazil na domovskej obrazovke. Údaje nikam neposiela ani neukladá."
            textSize = 18f
            setPadding(48, 64, 48, 48)
        })
    }
}
