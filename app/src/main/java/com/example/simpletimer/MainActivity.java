package com.example.simpletimer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_PICK_SOUND = 1;
    private static final String PREFS = "timer_prefs";

    private static final int IDLE = 0;
    private static final int RUNNING = 1;
    private static final int RINGING = 2;

    private int state = IDLE;

    private TextView display;
    private TextView soundLabel;
    private NumberPicker hoursPicker;
    private NumberPicker minutesPicker;
    private NumberPicker secondsPicker;
    private Button startButton;
    private Button stopButton;
    private Button chooseSoundButton;
    private Button defaultSoundButton;

    private CountDownTimer countDown;
    private MediaPlayer player;
    private String soundUri; // null = system default alarm sound

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadPrefs();
        display.setText(format(selectedMillis()));
        updateSoundLabel();
        updateUi();
    }

    // ---------------------------------------------------------------- UI

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private NumberPicker makePicker(int max) {
        NumberPicker p = new NumberPicker(this);
        p.setMinValue(0);
        p.setMaxValue(max);
        p.setWrapSelectorWheel(true);
        return p;
    }

    private LinearLayout labelled(NumberPicker picker, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(picker);
        TextView t = new TextView(this);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        col.addView(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(lp);
        return col;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        display = new TextView(this);
        display.setTextSize(56);
        display.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        display.setGravity(Gravity.CENTER);
        root.addView(display);

        // Timer setting: hours / minutes / seconds
        hoursPicker = makePicker(23);
        minutesPicker = makePicker(59);
        secondsPicker = makePicker(59);
        NumberPicker.OnValueChangeListener changed = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (state == IDLE) {
                    display.setText(format(selectedMillis()));
                }
            }
        };
        hoursPicker.setOnValueChangedListener(changed);
        minutesPicker.setOnValueChangedListener(changed);
        secondsPicker.setOnValueChangedListener(changed);

        LinearLayout pickers = new LinearLayout(this);
        pickers.setOrientation(LinearLayout.HORIZONTAL);
        pickers.setPadding(0, dp(24), 0, dp(24));
        pickers.addView(labelled(hoursPicker, "hours"));
        pickers.addView(labelled(minutesPicker, "minutes"));
        pickers.addView(labelled(secondsPicker, "seconds"));
        root.addView(pickers);

        // Sound selection
        soundLabel = new TextView(this);
        soundLabel.setGravity(Gravity.CENTER);
        soundLabel.setTextSize(16);
        root.addView(soundLabel);

        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setPadding(0, dp(8), 0, dp(24));
        chooseSoundButton = new Button(this);
        chooseSoundButton.setText("Choose sound");
        defaultSoundButton = new Button(this);
        defaultSoundButton.setText("Default sound");
        soundRow.addView(chooseSoundButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        soundRow.addView(defaultSoundButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(soundRow);

        // Start / Stop
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        startButton = new Button(this);
        startButton.setText("Start");
        startButton.setTextSize(20);
        stopButton = new Button(this);
        stopButton.setText("Stop");
        stopButton.setTextSize(20);
        controls.addView(startButton, new LinearLayout.LayoutParams(0, dp(64), 1f));
        controls.addView(stopButton, new LinearLayout.LayoutParams(0, dp(64), 1f));
        root.addView(controls);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAll();
            }
        });
        chooseSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickSound();
            }
        });
        defaultSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundUri = null;
                savePrefs();
                updateSoundLabel();
            }
        });

        setContentView(root);
    }

    private void updateUi() {
        boolean idle = state == IDLE;
        startButton.setEnabled(idle);
        stopButton.setEnabled(!idle);
        hoursPicker.setEnabled(idle);
        minutesPicker.setEnabled(idle);
        secondsPicker.setEnabled(idle);
        chooseSoundButton.setEnabled(idle);
        defaultSoundButton.setEnabled(idle);
        if (state == RINGING) {
            display.setText("Time's up!");
        }
    }

    private void updateSoundLabel() {
        String name = "Default alarm sound";
        if (soundUri != null) {
            String n = displayName(Uri.parse(soundUri));
            name = n != null ? n : "Custom sound";
        }
        soundLabel.setText("Sound: " + name);
    }

    private String displayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME},
                    null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (Exception ignored) {
            // fall through
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    // ------------------------------------------------------------- Timer

    private long selectedMillis() {
        long seconds = hoursPicker.getValue() * 3600L
                + minutesPicker.getValue() * 60L
                + secondsPicker.getValue();
        return seconds * 1000L;
    }

    private String format(long millis) {
        long totalSeconds = (millis + 999) / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    private void startTimer() {
        long total = selectedMillis();
        if (total <= 0) {
            Toast.makeText(this, "Set a time first", Toast.LENGTH_SHORT).show();
            return;
        }
        savePrefs();
        stopAlarm();
        state = RUNNING;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        display.setText(format(total));
        countDown = new CountDownTimer(total, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
                display.setText(format(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                countDown = null;
                state = RINGING;
                startAlarm();
                updateUi();
            }
        };
        countDown.start();
        updateUi();
    }

    private void stopAll() {
        if (countDown != null) {
            countDown.cancel();
            countDown = null;
        }
        stopAlarm();
        state = IDLE;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        display.setText(format(selectedMillis()));
        updateUi();
    }

    // ------------------------------------------------------------- Sound

    private Uri defaultAlarmUri() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return uri;
    }

    private MediaPlayer tryPlay(Uri uri) {
        if (uri == null) {
            return null;
        }
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mp.setDataSource(this, uri);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
            return mp;
        } catch (Exception e) {
            mp.release();
            return null;
        }
    }

    private void startAlarm() {
        stopAlarm();
        if (soundUri != null) {
            player = tryPlay(Uri.parse(soundUri));
            if (player == null) {
                Toast.makeText(this, "Couldn't play your sound, using default",
                        Toast.LENGTH_LONG).show();
            }
        }
        if (player == null) {
            player = tryPlay(defaultAlarmUri());
        }
    }

    private void stopAlarm() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
                // already stopped
            }
            player.release();
            player = null;
        }
    }

    private void pickSound() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_PICK_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_SOUND && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Some providers don't allow persisting; it will still work this session.
            }
            soundUri = uri.toString();
            savePrefs();
            updateSoundLabel();
        }
    }

    // ------------------------------------------------------- Persistence

    private void savePrefs() {
        SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        e.putInt("hours", hoursPicker.getValue());
        e.putInt("minutes", minutesPicker.getValue());
        e.putInt("seconds", secondsPicker.getValue());
        if (soundUri == null) {
            e.remove("sound");
        } else {
            e.putString("sound", soundUri);
        }
        e.apply();
    }

    private void loadPrefs() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        hoursPicker.setValue(p.getInt("hours", 0));
        minutesPicker.setValue(p.getInt("minutes", 5));
        secondsPicker.setValue(p.getInt("seconds", 0));
        soundUri = p.getString("sound", null);
    }

    @Override
    protected void onDestroy() {
        if (countDown != null) {
            countDown.cancel();
            countDown = null;
        }
        stopAlarm();
        super.onDestroy();
    }
}
