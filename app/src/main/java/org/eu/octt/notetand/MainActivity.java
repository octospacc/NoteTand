package org.eu.octt.notetand;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class MainActivity extends CustomActivity {
    ListView listNotes;
    List<String> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NoteManager.setup(this);

        listNotes = findViewById(R.id.list_notes);
//        var notesList = NoteManager.getAllNoteNames(); // new ArrayList<String>();
//        var notesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notesList);
//        listNotes.setAdapter(notesAdapter);

//        // Use Android/data/packagename/files/notes structure
//        var notesDirectory = new File(getExternalFilesDir(null), "notes");
//        if (!notesDirectory.exists()) {
//            notesDirectory.mkdirs();
//        }

        // for (var file : notesDirectory.listFiles()) {
//        for (var file : NoteManager.notesDirectory.listFiles()) {
//            if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
//                notesList.add(file.getName());
//            }
//        }
        // notesAdapter.notifyDataSetChanged();

        listNotes.setOnItemClickListener((parent, view, position, id) ->
            launchNote(notesList.get(position), false));
    }

    @Override
    protected void onStart() {
        super.onStart();

        notesList = NoteManager.getAllNoteNames(); // new ArrayList<String>();
        var notesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notesList);
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
}
