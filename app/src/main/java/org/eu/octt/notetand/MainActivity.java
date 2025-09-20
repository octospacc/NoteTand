package org.eu.octt.notetand;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class MainActivity extends CustomActivity {
    ListView listNotes;
    List<String> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SettingsManager.getBlockCapture())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_main);
        NotesManager.setup(this);

        listNotes = findViewById(R.id.list_notes);
        listNotes.setOnItemClickListener((parent, view, position, id) -> launchNote(notesList.get(position), false));
    }

    @Override
    protected void onStart() {
        super.onStart();

        notesList = NotesManager.getAllNoteNames();
        var notesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, notesList) {
            @SuppressLint("SetTextI18n")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                var view = super.getView(position, convertView, parent);

                TextView text1 = view.findViewById(android.R.id.text1);
                text1.setMaxLines(1);
                text1.setEllipsize(TextUtils.TruncateAt.END);

                TextView text2 = view.findViewById(android.R.id.text2);
                text2.setMaxLines(1);
                text2.setEllipsize(TextUtils.TruncateAt.END);

                var preview = readFirstXChars(new File(NotesManager.getNotesDirectory(), notesList.get(position)), 256);
                if (preview.length() == 256)
                    preview = preview.trim() + "...";
                text2.setText(preview);

                return view;
            }
        };
        listNotes.setAdapter(notesAdapter);
        notesAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        var id = item.getItemId();
        if (id == R.id.action_new) {
            launchNote(null, true);
        } else if (id == R.id.action_receive) {
            startActivity(new Intent(this, ReceiveActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_about) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gitlab.com/octospacc/NoteTand")));
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    void launchNote(String name, boolean isNew) {
        startActivity(
            new Intent(this, NoteActivity.class)
                .putExtra("is_new", isNew)
                .putExtra("note_name", name)
        );
    }

    private String readFirstXChars(File file, int maxChars) {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int charsRead = 0;
            int c;
            while (charsRead < maxChars && (c = reader.read()) != -1) {
                result.append((char) c);
                charsRead++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
