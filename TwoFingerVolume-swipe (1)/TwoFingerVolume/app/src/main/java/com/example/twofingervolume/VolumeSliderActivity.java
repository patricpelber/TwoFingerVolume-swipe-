package com.example.twofingervolume;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class VolumeSliderActivity extends Activity {

    private static final long AUTO_CLOSE_MS = 6000;

    private AudioManager audio;
    private SeekBar seekBar;
    private TextView label;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show even when the phone is locked
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        setFinishOnTouchOutside(true);

        audio = (AudioManager) getSystemService(AUDIO_SERVICE);

        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        label = new TextView(this);
        label.setTextSize(18f);
        label.setGravity(Gravity.CENTER);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button minus = new Button(this);
        minus.setText("-");
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                refresh();
            }
        });

        Button plus = new Button(this);
        plus.setText("+");
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                refresh();
            }
        });

        seekBar = new SeekBar(this);
        seekBar.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    updateLabel(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
                handler.removeCallbacks(closeRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
                scheduleClose();
            }
        });

        row.addView(minus);
        row.addView(seekBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(plus);

        root.addView(label);
        root.addView(row);
        setContentView(root);

        getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        refresh();
        scheduleClose();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refresh();
        scheduleClose();
    }

    private void refresh() {
        int cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBar.setProgress(cur);
        updateLabel(cur);
        scheduleClose();
    }

    private void updateLabel(int cur) {
        label.setText("Media volume: " + cur + " / " + audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
    }

    private void scheduleClose() {
        handler.removeCallbacks(closeRunnable);
        handler.postDelayed(closeRunnable, AUTO_CLOSE_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(closeRunnable);
        super.onDestroy();
    }
}
