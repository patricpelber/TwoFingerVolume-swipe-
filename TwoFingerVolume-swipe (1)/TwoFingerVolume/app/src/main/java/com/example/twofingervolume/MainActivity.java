package com.example.twofingervolume;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView status;
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Two Finger Volume");
        title.setTextSize(24f);
        layout.addView(title);

        status = new TextView(this);
        status.setTextSize(18f);
        status.setPadding(0, pad / 2, 0, pad / 2);
        layout.addView(status);

        addText("Do these 5 steps once (Xiaomi / Redmi / MIUI):");

        addText("STEP 1: Turn the app ON.\nTap the button, find \"Two Finger Volume\" and switch it ON. "
                + "If the switch is grey: use STEP 5 first, then come back.");
        addButton("1. Open Accessibility Settings", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        addText("STEP 2: Turn Autostart ON.\nSwitch ON the toggle next to \"Two Finger Volume\".");
        addButton("2. Open Autostart", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFirstWorking(autostartIntent(), appInfoIntent());
            }
        });

        addText("STEP 3: Battery = No restrictions.\nChoose \"No restrictions\" for this app.");
        addButton("3. Open Battery Settings", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFirstWorking(batteryIntentMiui(),
                        new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                        appInfoIntent());
            }
        });

        addText("STEP 4: Allow pop-ups and lock screen.\nTurn ON \"Display pop-up windows while running in the background\" "
                + "and \"Show on Lock screen\".");
        addButton("4. Open MIUI Permissions", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFirstWorking(permEditorIntent("com.miui.permcenter.permissions.PermissionsEditorActivity"),
                        permEditorIntent("com.miui.permcenter.permissions.AppPermissionsEditorActivity"),
                        appInfoIntent());
            }
        });

        addText("STEP 5: Only if the switch in step 1 is grey.\nTap the 3 dots at the top right and choose "
                + "\"Allow restricted settings\". If a button doesn't open the right screen, go to "
                + "Settings > Apps > Manage apps > Two Finger Volume.");
        addButton("5. Open App Info", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFirstWorking(appInfoIntent());
            }
        });

        addText("FINAL TIP: open the Recent apps screen, then pull this app's card DOWN (or tap the lock icon) "
                + "so it shows a lock. This stops MIUI from killing it. Never swipe it away.");

        addText("HOW TO USE:\n"
                + "- POWER BUTTON: press it twice quickly (screen off, then on). A volume slider appears.\n"
                + "- FINGERPRINT SWIPE: swipe UP on the fingerprint sensor = volume up, DOWN = volume down. "
                + "This only works if your phone supports it. Many phones with the sensor in the power button do not.\n"
                + "- TWO FINGERS: you will see a thin grey strip in the middle of the right edge. "
                + "Put one finger on it and a second finger anywhere. A volume panel appears.\n"
                + "- To stop the app completely, turn it OFF in Accessibility settings.");

        addButton("Test the volume slider now", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VolumeSliderActivity.class));
            }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addText(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(15f);
        t.setPadding(0, 24, 0, 8);
        layout.addView(t);
    }

    private void addButton(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(listener);
        layout.addView(b);
    }

    private Intent appInfoIntent() {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
    }

    private Intent autostartIntent() {
        Intent i = new Intent();
        i.setComponent(new ComponentName("com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        return i;
    }

    private Intent batteryIntentMiui() {
        Intent i = new Intent();
        i.setComponent(new ComponentName("com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"));
        i.putExtra("package_name", getPackageName());
        i.putExtra("package_label", getString(R.string.app_name));
        return i;
    }

    private Intent permEditorIntent(String className) {
        Intent i = new Intent("miui.intent.action.APP_PERM_EDITOR");
        i.setClassName("com.miui.securitycenter", className);
        i.putExtra("extra_pkgname", getPackageName());
        return i;
    }

    // Try each screen in order until one opens.
    private void startFirstWorking(Intent... intents) {
        for (Intent i : intents) {
            try {
                startActivity(i);
                return;
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(this,
                "Could not open it. Go to Settings > Apps > Manage apps > Two Finger Volume.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        status.setText(isServiceEnabled()
                ? "Status: RUNNING (turn off in Accessibility settings)"
                : "Status: NOT enabled yet");
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(getPackageName());
    }
}
