package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import java.util.List;
import java.util.Locale;

/**
 * dock_apps entries that name a TV input instead of an app: "input:HDMI 1" (the port's name as the Inputs menu
 * shows it, spaces and case ignored) or "input:" plus the input's id from `dumpsys tv_input`. The dock tile
 * switches straight to that input, like its row in the Inputs menu.
 */
final class DockInputs {
    static final String PREFIX = "input:";

    private DockInputs() { }

    static boolean isInput(String dockEntry) {
        return dockEntry.startsWith(PREFIX) && dockEntry.length() > PREFIX.length();
    }

    static boolean any(List<String> dockEntries) {
        for (String e : dockEntries) if (isInput(e)) return true;
        return false;
    }

    /** True when the part after "input:" names this input: its exact id, or its port name. */
    static boolean matches(String dockEntry, String inputId, String portLabel) {
        String spec = dockEntry.substring(PREFIX.length()).trim();
        return spec.equals(inputId) || (!fold(spec).isEmpty() && fold(spec).equals(fold(portLabel)));
    }

    /** The id of the input an "input:..." entry names, or null while the TV has no such input (yet: at boot
     *  HDMI inputs register after the launcher is up). Inputs hidden in Settings > Inputs don't count. */
    static String findId(Context ctx, String dockEntry) {
        TvInputManager tim = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null || !isInput(dockEntry)) return null;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (!info.isPassthroughInput() || info.isHidden(ctx)) continue;
            if (matches(dockEntry, info.getId(), portLabel(ctx, info))) return info.getId();
        }
        return null;
    }

    /** What the Inputs menu sends to switch to a passthrough input: ACTION_VIEW on its channel URI, which the
     *  Live TV app handles. */
    static Intent viewIntent(String inputId) {
        Uri uri = TvContract.buildChannelUriForPassthroughInput(inputId);
        return new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "vnd.android.cursor.item/channel")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** The port's name as the Inputs menu shows it: "HDMI 1" even where the TV labels the port just "1". */
    static String portLabel(Context ctx, TvInputInfo info) {
        CharSequence l = info.loadLabel(ctx);
        String label = l == null ? info.getId() : l.toString();
        if (info.getType() == TvInputInfo.TYPE_HDMI && !label.toUpperCase(Locale.ROOT).contains("HDMI")) {
            label = "HDMI " + label;
        }
        return label;
    }

    private static String fold(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
