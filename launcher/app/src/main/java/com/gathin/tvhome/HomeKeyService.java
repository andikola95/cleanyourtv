package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.Collections;
import java.util.Map;

/**
 * Sends the remote's Home button to this launcher on remotes that emit KEYCODE_ALL_APPS for Home
 * (Xiaomi sticks and boxes). Android TV 9 routes that key only to the system-image launcher, so once
 * the stock launcher is disabled the button does nothing. Every other key passes through untouched.
 *
 * Enable over adb:
 *   settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
 *   settings put secure accessibility_enabled 1
 */
public class HomeKeyService extends AccessibilityService {

    /** Remote buttons that should open Settings (config.xml, settings_key_scancodes and settings_key_keycodes). */
    private int[] settingsScancodes = new int[0];
    private int[] settingsKeycodes = new int[0];
    private boolean settingsPanel;
    /** app_keys, the same map InputKeyService reads, for setups that run only this service. */
    private Map<Integer, String> appKeys = Collections.emptyMap();
    private final Handler handler = new Handler(Looper.getMainLooper());
    /** config.xml power_on_input on waking from standby; a cold boot is PowerOnInput.onBoot. */
    private boolean screenOnRegistered;
    private final BroadcastReceiver screenOn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerOnInput.onScreenOn(context);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (PowerOnInput.configured(this) != null) {
            if (!screenOnRegistered) registerReceiver(screenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
            screenOnRegistered = true;
            // No SCREEN_ON reaches a service that connects during boot.
            PowerOnInput.onBoot(this);
        }
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        audioResetKeycodes = getResources().getIntArray(R.array.audio_reset_keycodes);
        // Keycodes and the panel choice can also come from a theme; theme.sh restarts this service.
        Config config = Config.load(this);
        settingsKeycodes = config.settingsKeycodes;
        settingsPanel = config.settingsKeyPanel;
        appKeys = RemoteKeys.parseAppKeys(config.appKeys);
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        for (int code : settingsKeycodes) if (event.getKeyCode() == code) return true;
        return false;
    }

    /** config.xml audio_reset_*: buttons that flip the TV's audio output to its speakers and back to the soundbar. */
    private int[] audioResetKeycodes = new int[0];
    /** How long the output stays on the speakers: TCL's audio service takes about half a second to switch. */
    private static final long AUDIO_RESET_MS = 1500;

    private boolean isAudioResetKey(KeyEvent event) {
        for (int code : audioResetKeycodes) if (event.getKeyCode() == code) return true;
        return false;
    }

    /** What switching the audio output to the TV speakers and back in Settings does; TCL's system service
     *  watches the setting and redoes the ARC/eARC hand-over to the soundbar. */
    private void resetAudio() {
        String setting = getString(R.string.audio_reset_setting);
        int speaker = getResources().getInteger(R.integer.audio_reset_speaker);
        int soundbar = getResources().getInteger(R.integer.audio_reset_soundbar);
        try {
            Settings.Global.putInt(getContentResolver(), setting, speaker);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.audio_reset_denied, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.audio_reset, Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> Settings.Global.putInt(getContentResolver(), setting, soundbar), AUDIO_RESET_MS);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (isAudioResetKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) resetAudio();
            return true;
        }
        // The stock launcher handled the remote's Settings button; with it disabled the button does nothing.
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = settingsPanel ? new Intent(this, QuickSettingsActivity.class)
                        : new Intent(Settings.ACTION_SETTINGS);
                startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return true;
        }
        String app = appKeys.get(event.getKeyCode());
        if (app != null) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) RemoteKeys.launch(this, app);
            return true;
        }
        if (event.getKeyCode() != KeyEvent.KEYCODE_ALL_APPS) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            Intent home = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setPackage(getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(home);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (screenOnRegistered) unregisterReceiver(screenOn);
        screenOnRegistered = false;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
