package com.example.twofingervolume;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.FingerprintGestureController;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class TwoFingerService extends AccessibilityService {

    private static final long HIDE_DELAY_MS = 4000;
    // Power button pressed twice: screen OFF then ON within this time
    private static final long DOUBLE_PRESS_MS = 1800;
    private long lastScreenOff = 0;

    private WindowManager wm;
    private AudioManager audio;
    private View edgeStrip;
    private LinearLayout panel;
    private SeekBar seekBar;
    private boolean panelShown = false;
    private FingerprintGestureController.FingerprintGestureCallback fpCallback;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hidePanel();
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            long now = SystemClock.elapsedRealtime();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                lastScreenOff = now;
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (lastScreenOff > 0 && now - lastScreenOff < DOUBLE_PRESS_MS) {
                    lastScreenOff = 0;
                    // Phone locked -> slider screen that shows over the lock screen.
                    // Phone unlocked -> on-screen panel (works without extra MIUI permissions).
                    KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                    if (km != null && km.isKeyguardLocked()) {
                        openVolumeSlider();
                    } else {
                        showPanel();
                    }
                }
            }
        }
    };

    private void openVolumeSlider() {
        Intent i = new Intent(this, VolumeSliderActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        buildPanel();
        addEdgeStrip();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);

        setupFingerprintSwipe();
    }

    // Swipe up / down on the fingerprint sensor. Only works on phones whose
    // sensor supports gestures (most side-mounted power-button sensors do not).
    private void setupFingerprintSwipe() {
        try {
            FingerprintGestureController fp = getFingerprintGestureController();
            fpCallback = new FingerprintGestureController.FingerprintGestureCallback() {
                @Override
                public void onGestureDetected(int gesture) {
                    if (gesture == FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP) {
                        showPanel();
                        change(AudioManager.ADJUST_RAISE);
                    } else if (gesture == FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN) {
                        showPanel();
                        change(AudioManager.ADJUST_LOWER);
                    }
                }
            };
            fp.registerFingerprintGestureCallback(fpCallback, handler);
            if (!fp.isGestureDetectionAvailable()) {
                Toast.makeText(this,
                        "Fingerprint swipe is not supported on this phone. Power button double press still works.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception ignored) {
        }
    }

    // Thin strip on the right edge. Two fingers on it -> show volume panel.
    private void addEdgeStrip() {
        edgeStrip = new View(this);
        edgeStrip.setBackgroundColor(Color.parseColor("#40808080"));
        edgeStrip.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getPointerCount() >= 2) {
                    showPanel();
                }
                return true;
            }
        });

        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.35f);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                dp(24),
                height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        wm.addView(edgeStrip, lp);
    }

    private void buildPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(10);
        panel.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E6222222"));
        bg.setCornerRadius(dp(28));
        panel.setBackground(bg);

        TextView minus = makeButton("-");
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                change(AudioManager.ADJUST_LOWER);
            }
        });

        TextView plus = makeButton("+");
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                change(AudioManager.ADJUST_RAISE);
            }
        });

        TextView close = makeButton("X");
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePanel();
            }
        });

        seekBar = new SeekBar(this);
        seekBar.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                dp(170), LinearLayout.LayoutParams.WRAP_CONTENT));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
                handler.removeCallbacks(hideRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                scheduleHide();
            }
        });

        panel.addView(minus);
        panel.addView(seekBar);
        panel.addView(plus);
        panel.addView(close);

        // Tap outside the panel closes it.
        panel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    hidePanel();
                }
                return false;
            }
        });
    }

    private TextView makeButton(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(dp(48));
        t.setMinHeight(dp(48));
        return t;
    }

    private void showPanel() {
        if (!panelShown) {
            seekBar.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            seekBar.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            lp.y = dp(140);
            wm.addView(panel, lp);
            panelShown = true;
        }
        scheduleHide();
    }

    private void hidePanel() {
        handler.removeCallbacks(hideRunnable);
        if (panelShown) {
            try {
                wm.removeView(panel);
            } catch (IllegalArgumentException ignored) {
            }
            panelShown = false;
        }
    }

    private void scheduleHide() {
        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, HIDE_DELAY_MS);
    }

    private void change(int direction) {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
        seekBar.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
        scheduleHide();
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        try {
            if (fpCallback != null) {
                getFingerprintGestureController().unregisterFingerprintGestureCallback(fpCallback);
            }
        } catch (Exception ignored) {
        }
        try {
            unregisterReceiver(screenReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        hidePanel();
        if (edgeStrip != null) {
            try {
                wm.removeView(edgeStrip);
            } catch (IllegalArgumentException ignored) {
            }
            edgeStrip = null;
        }
        super.onDestroy();
    }
}
