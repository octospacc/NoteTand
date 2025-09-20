package org.eu.octt.notetand;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;

import java.lang.reflect.Method;

abstract public class CustomActivity extends Activity {
    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsManager.setup(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            switch (SettingsManager.getTheme()) {
                case "holo_dark":
                    setTheme(android.R.style.Theme_Holo);
                    break;
                case "holo_light":
                    setTheme(android.R.style.Theme_Holo_Light);
                    break;
                case "material_dark":
                    setTheme(android.R.style.Theme_Material);
                    break;
                case "material_light":
                    setTheme(android.R.style.Theme_Material_Light);
                    break;
                default:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        setTheme(android.R.style.Theme_DeviceDefault_DayNight);
                    break;
            }
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
            try {
                Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                method.setAccessible(true);
                method.invoke(menu, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    void setActionBarBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
