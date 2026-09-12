package sk.restartaa;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String ANDROID_AUTO = "com.google.android.projection.gearhead";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(48, 48, 48, 48);
        box.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView title = new TextView(this);
        title.setText("Reštart Android Auto");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(20, 30, 45));
        title.setGravity(Gravity.CENTER);
        box.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView help = new TextView(this);
        help.setText("Na otvorenej obrazovke stlačte Vynútiť zastavenie. Android Auto sa znovu spustí pri pripojení k vozidlu.");
        help.setTextSize(17);
        help.setTextColor(Color.DKGRAY);
        help.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.setMargins(0, 32, 0, 40);
        box.addView(help, hp);

        Button open = new Button(this);
        open.setText("OTVORIŤ ANDROID AUTO");
        open.setOnClickListener(v -> openAndroidAutoSettings());
        box.addView(open, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(box);

        new Handler(Looper.getMainLooper()).postDelayed(this::openAndroidAutoSettings, 450);
    }

    private void openAndroidAutoSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + ANDROID_AUTO));
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "Android Auto sa nepodarilo nájsť.", Toast.LENGTH_LONG).show();
        }
    }
}
