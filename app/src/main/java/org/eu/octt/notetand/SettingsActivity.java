package org.eu.octt.notetand;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsActivity extends CustomActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        // getFragmentManager().beginTransaction().add(new SettingsFragment(), "").commit();
//        // addPreferencesFromResource(R.xml.preferences);
//
//    }

//    class SettingsFragment extends Fragment {
//
//    }

    private ListView listView;
    private ArrayList<View> settingViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = new ListView(this);

        settingViews.add(createCheckboxSetting(getString(R.string.censor_mac), "censor_mac", true));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            settingViews.add(createSpinnerSetting(getString(R.string.app_theme), "theme", new String[]{"system", "material_dark", "material_light", "holo_dark", "holo_light"}, "system"));
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            settingViews.add(createSpinnerSetting(getString(R.string.app_theme), "theme", new String[]{"system", "holo_dark", "holo_light"}, "system"));

        // settingViews.add(createNumberSetting(getString(R.string.font_size), "font_size"));

        // Adapter to wrap views into ListView
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return settingViews.size();
            }

            @Override
            public Object getItem(int position) {
                return settingViews.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return settingViews.get(position);
            }
        });

        setContentView(listView);
    }

    private View createCheckboxSetting(String label, String key, boolean defaultValue) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);

        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextSize(16);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        CheckBox checkBox = new CheckBox(this);
        checkBox.setChecked(SettingsManager.prefs.getBoolean(key, defaultValue));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsManager.prefs.edit().putBoolean(key, isChecked).apply();
        });

        layout.addView(textView);
        layout.addView(checkBox);
        return layout;
    }

    private View createSpinnerSetting(String label, String key, String[] options, String defaultValue) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView textView = new TextView(this);
        textView.setText(label);
        textView.setTextSize(16);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String currentValue = SettingsManager.prefs.getString(key, defaultValue);
        int selectedIndex = Arrays.asList(options).indexOf(currentValue);
        spinner.setSelection(Math.max(selectedIndex, 0));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SettingsManager.prefs.edit().putString(key, options[position]).apply();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        layout.addView(textView);
        layout.addView(spinner);
        return layout;
    }
}
