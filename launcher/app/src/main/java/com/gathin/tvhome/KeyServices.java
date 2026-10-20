package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Brings this launcher's key services (HomeKeyService, InputKeyService) back when the TV left them enabled but
 * unbound. TCL Google TV 12 (G10) blocks binding sideloaded services at boot now and then ("[TclAppBoot] forbid bind
 * service", APP_AUTO_START reset to ignore at every boot), and then the remote's Settings, Input and mapped buttons
 * do nothing until the services are toggled. Writing the enabled list again from the foreground launcher rebinds
 * them; it needs WRITE_SECURE_SETTINGS (build-and-install.sh grants it) and does nothing without it.
 */
final class KeyServices {
    private static final long MIN_INTERVAL_MS = 10_000;
    private static long lastAttempt = -MIN_INTERVAL_MS;

    private KeyServices() { }

    static void rebindIfDropped(Context ctx) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastAttempt < MIN_INTERVAL_MS) return;
        String enabled = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        Set<ComponentName> ours = ownComponents(ctx, enabled);
        if (ours.isEmpty()) return;   // the owner never enabled them: nothing to bring back
        AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return;
        // Despite its name this lists the services that are bound and running, not the enabled setting.
        List<AccessibilityServiceInfo> running = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : running) {
            ours.remove(ComponentName.unflattenFromString(info.getId()));
        }
        if (ours.isEmpty()) return;
        lastAttempt = now;
        try {
            Settings.Secure.putString(ctx.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "");
            Settings.Secure.putString(ctx.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, enabled);
            Settings.Secure.putInt(ctx.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (SecurityException ignored) {
        }
    }

    /** This app's components in a colon-separated enabled-services list. */
    private static Set<ComponentName> ownComponents(Context ctx, String enabled) {
        Set<ComponentName> out = new HashSet<>();
        if (enabled == null) return out;
        for (String entry : enabled.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(entry.trim());
            if (c != null && c.getPackageName().equals(ctx.getPackageName())) out.add(c);
        }
        return out;
    }
}
