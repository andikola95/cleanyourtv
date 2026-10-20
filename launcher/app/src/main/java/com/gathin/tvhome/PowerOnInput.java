package com.gathin.tvhome;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * config.xml power_on_input: switch to that input whenever the TV turns on. HomeKeyService handles waking from
 * standby (SCREEN_ON); a cold boot is handled by whichever comes first, the launcher showing Home or the key service
 * connecting, because TCL's boot manager sometimes refuses to bind the service until the launcher rebinds it
 * (KeyServices). Android's boot counter makes sure only one of them switches per boot.
 */
final class PowerOnInput {
    /** Coming up this soon after boot counts as the TV powering on. */
    private static final long BOOT_WINDOW_MS = 3 * 60 * 1000;
    /** At boot the HDMI inputs register a few seconds after Home is up; look again this often, this many times. */
    private static final long RETRY_MS = 2000;
    private static final int RETRIES = 20;
    private static final String PREFS = "power_on_input", HANDLED_BOOT = "handled_boot";
    private static final Handler retries = new Handler(Looper.getMainLooper());

    private PowerOnInput() { }

    static String configured(Context ctx) {
        String entry = ctx.getString(R.string.power_on_input).trim();
        return DockInputs.isInput(entry) ? entry : null;
    }

    /** Switches once per boot, while the boot is recent; every later call for the same boot does nothing. */
    static void onBoot(Context ctx) {
        String entry = configured(ctx);
        if (entry == null || SystemClock.elapsedRealtime() > BOOT_WINDOW_MS) return;
        int boot = Settings.Global.getInt(ctx.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (boot < 0 || prefs.getInt(HANDLED_BOOT, -1) == boot) return;
        prefs.edit().putInt(HANDLED_BOOT, boot).apply();
        open(ctx.getApplicationContext(), entry, 0);
    }

    /** Switches now (the TV woke from standby), dropping any switch still waiting for its input. */
    static void onScreenOn(Context ctx) {
        String entry = configured(ctx);
        if (entry == null) return;
        retries.removeCallbacksAndMessages(null);
        open(ctx.getApplicationContext(), entry, 0);
    }

    private static void open(Context ctx, String entry, int attempt) {
        String id = DockInputs.findId(ctx, entry);
        if (id == null) {
            if (attempt < RETRIES) retries.postDelayed(() -> open(ctx, entry, attempt + 1), RETRY_MS);
            return;
        }
        try {
            ctx.startActivity(DockInputs.viewIntent(id));
        } catch (ActivityNotFoundException ignored) {
        }
    }
}
