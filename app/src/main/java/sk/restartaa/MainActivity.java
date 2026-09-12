package sk.restartaa;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
        box.addView(title, fullWidth());

        TextView help = new TextView(this);
        help.setText("1. Otvorte nastavenia a stlačte Vynútiť zastavenie.\n\n2. Vráťte sa sem a stlačte Spustiť Android Auto.");
        help.setTextSize(17);
        help.setTextColor(Color.DKGRAY);
        help.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = fullWidth();
        hp.setMargins(0, 32, 0, 32);
        box.addView(help, hp);

        Button stop = new Button(this);
        stop.setText("1. ZASTAVIŤ ANDROID AUTO");
        stop.setOnClickListener(v -> openAndroidAutoSettings());
        box.addView(stop, fullWidth());

        Button start = new Button(this);
        start.setText("2. SPUSTIŤ ANDROID AUTO");
        start.setOnClickListener(v -> startAndroidAuto());
        LinearLayout.LayoutParams sp = fullWidth();
        sp.setMargins(0, 20, 0, 0);
        box.addView(start, sp);

        setContentView(box);
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private void startAndroidAuto() {
        PackageManager pm = getPackageManager();

        Intent launcher = pm.getLaunchIntentForPackage(ANDROID_AUTO);
        if (tryStart(launcher)) {
            Toast.makeText(this, "Android Auto bolo spustené.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            PackageInfo info = pm.getPackageInfo(ANDROID_AUTO, PackageManager.GET_ACTIVITIES);
            List<ActivityInfo> candidates = new ArrayList<>();
            if (info.activities != null) {
                for (ActivityInfo activity : info.activities) {
                    if (activity.exported && activity.enabled) candidates.add(activity);
                }
            }
            candidates.sort(Comparator.comparingInt(this::activityScore).reversed());

            for (ActivityInfo activity : candidates) {
                Intent explicit = new Intent();
                explicit.setClassName(ANDROID_AUTO, activity.name);
                explicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (tryStart(explicit)) {
                    Toast.makeText(this, "Android Auto bolo aktivované.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (Exception ignored) {
            // Fallback message below.
        }

        Toast.makeText(this,
                "Táto verzia Android Auto nemá spustiteľnú obrazovku. Pripojte telefón k vozidlu.",
                Toast.LENGTH_LONG).show();
    }

    private int activityScore(ActivityInfo activity) {
        String name = activity.name.toLowerCase(Locale.ROOT);
        int score = 0;
        if (name.contains("defaultsettings")) score += 100;
        if (name.contains("launcher")) score += 80;
        if (name.contains("settings")) score += 60;
        if (name.contains("companion")) score += 30;
        if (name.contains("permission")) score -= 50;
        return score;
    }

    private boolean tryStart(Intent intent) {
        if (intent == null) return false;
        try {
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
