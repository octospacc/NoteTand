package org.eu.octt.notetand;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    static SharedPreferences prefs;

    static void setup(Context context) {
        if (prefs == null)
            prefs = context.getSharedPreferences("settings", MODE_PRIVATE);
    }

    static boolean getCensorMac() {
        return prefs.getBoolean("censor_mac", true);
    }

    static String getTheme() {
        return prefs.getString("theme", "system");
    }
}
