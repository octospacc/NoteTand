package org.eu.octt.notetand;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    static final String[] THEMES_FULL = {"system", "material_dark", "material_light", "holo_dark", "holo_light"};
    static final String[] THEMES_LEGACY = {"system", "holo_dark", "holo_light"};
    static final String[] FONT_TYPES = {"default", "sans_serif", "serif", "monospace"};
    static final String[] KEYBOARD_MODES = {"autocorrect", "normal", "no_suggestions", "privacy"};

    static SharedPreferences prefs;

    static void setup(Context context) {
        if (prefs == null)
            prefs = context.getSharedPreferences("settings", MODE_PRIVATE);
    }

    static boolean getCensorMac() {
        return prefs.getBoolean("censor_mac", true);
    }

    static String getTheme() {
        return prefs.getString("theme", THEMES_FULL[0]);
    }

    static int getFontSize() {
        return prefs.getInt("font_size", 0);
    }

    static String getFontType() {
        return prefs.getString("font_type", FONT_TYPES[0]);
    }

    static String getKeyboardMode() {
        return prefs.getString("keyboard_mode", KEYBOARD_MODES[0]);
    }
}
