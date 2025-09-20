package org.eu.octt.notetand;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsActivity extends CustomActivity {
    private final ArrayList<View> settingViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var listView = new ListView(this);

        // autosave
        // default_location

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            settingViews.add(createSpinnerSetting(getString(R.string.app_theme), "theme", SettingsManager.THEMES_FULL));
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            settingViews.add(createSpinnerSetting(getString(R.string.app_theme), "theme", SettingsManager.THEMES_LEGACY));

        settingViews.add(createNumberSetting(getString(R.string.font_size), "font_size", 0, 1));

        settingViews.add(createSpinnerSetting(getString(R.string.font_type), "font_type", SettingsManager.FONT_TYPES));

        settingViews.add(createSpinnerSetting(getString(R.string.keyboard_mode), "keyboard_mode", SettingsManager.KEYBOARD_MODES));

        settingViews.add(createCheckboxSetting(getString(R.string.censor_mac), "censor_mac", true));

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
        var layout = createLayout(LinearLayout.HORIZONTAL);

        var textView = new TextView(this);
        textView.setText(label);
        // textView.setTextSize(16);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        var checkBox = new CheckBox(this);
        checkBox.setChecked(SettingsManager.prefs.getBoolean(key, defaultValue));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> SettingsManager.prefs.edit().putBoolean(key, isChecked).apply());

        layout.addView(textView);
        layout.addView(checkBox);
        return layout;
    }

    private View createNumberSetting(String label, String key, int defaultValue, int min) {
        var layout = createLayout(LinearLayout.HORIZONTAL);

        var textView = new TextView(this);
        textView.setText(label);
        // textView.setTextSize(16);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        var editNumber = new EditText(this);
        editNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        editNumber.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            try {
                String input = dest.toString() + source.toString();
                int value = Integer.parseInt(input);
                if (value >= min) {
                    return null; // Accept the input
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
            return ""; // Reject the input
        }});
        var number = SettingsManager.prefs.getInt(key, defaultValue);
        if (number >= min)
            editNumber.setText(String.valueOf(number));
        editNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                var text = s.toString();
                SettingsManager.prefs.edit().putInt(key, !text.isEmpty() ? Integer.parseInt(text) : 0).apply();
            }
        });

        layout.addView(textView);
        layout.addView(editNumber);
        return layout;
    }

    private View createSpinnerSetting(String label, String key, String[] options) {
        var layout = createLayout(LinearLayout.VERTICAL);

        var textView = new TextView(this);
        textView.setText(label);
        // textView.setTextSize(16);

        var spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        var currentValue = SettingsManager.prefs.getString(key, options[0]);
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

    private LinearLayout createLayout(int orientation) {
        var layout = new LinearLayout(this);
        layout.setOrientation(orientation);
        layout.setPadding(16, 16, 16, 16);
        return layout;
    }
}
