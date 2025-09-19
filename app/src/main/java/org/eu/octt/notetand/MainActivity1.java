package org.eu.octt.notetand;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity1 extends Activity {

    private static final int REQUEST_ENABLE_BT = 1001;
    private static final int REQUEST_PERMISSIONS = 1002;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1003;

    private ListView listNotes;
    private TextView txtStatus;
    private TextView txtNoteCount;
    private Button btnSync;
    private Button btnAddNote;

    private ArrayList<String> notesList;
    private ArrayAdapter<String> notesAdapter;
    private File notesDirectory;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_1);

        initializeViews();
        setupNotesDirectory();
        checkPermissions();
        initializeBluetooth();
        loadNotes();
        setupListeners();
    }

    private void initializeViews() {
        listNotes = findViewById(R.id.list_notes);
        txtStatus = findViewById(R.id.txt_status);
        txtNoteCount = findViewById(R.id.txt_note_count);
        btnSync = findViewById(R.id.btn_sync);
        btnAddNote = findViewById(R.id.btn_add_note);

        notesList = new ArrayList<>();
        notesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notesList);
        listNotes.setAdapter(notesAdapter);

        // Register context menu for long press
        registerForContextMenu(listNotes);
    }

    private void setupNotesDirectory() {
        // Use Android/data/packagename/files/notes structure
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            notesDirectory = new File(externalFilesDir, "notes");
            if (!notesDirectory.exists()) {
                boolean created = notesDirectory.mkdirs();
                if (created) {
                    updateStatus("Notes directory created");
                } else {
                    updateStatus("Failed to create notes directory");
                }
            }
        } else {
            // Fallback to internal storage
            notesDirectory = new File(getFilesDir(), "notes");
            if (!notesDirectory.exists()) {
                notesDirectory.mkdirs();
            }
            updateStatus("Using internal storage");
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissionsNeeded = new ArrayList<>();

            // Storage permissions (for API < 30)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }

            // Location permission for Bluetooth scanning
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (!permissionsNeeded.isEmpty()) {
                requestPermissions(permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
            }
        }
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            updateStatus("Bluetooth not supported");
            btnSync.setEnabled(false);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            updateStatus("Bluetooth disabled");
            btnSync.setText("Enable BT");
        } else {
            updateStatus("Bluetooth ready");
            btnSync.setText("Sync");
        }
    }

    private void loadNotes() {
        notesList.clear();

        if (notesDirectory != null && notesDirectory.exists()) {
            File[] files = notesDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        String noteName = file.getName().replace(".txt", "");
                        notesList.add(noteName);
                    }
                }
            }
        }

        notesAdapter.notifyDataSetChanged();
        updateNoteCount();

        if (notesList.isEmpty()) {
            updateStatus("No notes found. Tap + to create one.");
        } else {
            updateStatus("Loaded " + notesList.size() + " notes");
        }
    }

    private void setupListeners() {
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewNote();
            }
        });

        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSyncButtonClick();
            }
        });

        listNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String noteName = notesList.get(position);
                openNote(noteName);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.list_notes) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            String noteName = notesList.get(info.position);
            menu.setHeaderTitle(noteName);
            menu.add(0, 1, 0, "Open");
            menu.add(0, 2, 0, "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final String noteName = notesList.get(info.position);

        switch (item.getItemId()) {
            case 1: // Open
                openNote(noteName);
                return true;
            case 2: // Delete
                new AlertDialog.Builder(this)
                        .setTitle("Delete Note")
                        .setMessage("Are you sure you want to delete '" + noteName + "'?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteNote(noteName);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void createNewNote() {
        // For now, create a simple numbered note
        String newNoteName = "Note_" + System.currentTimeMillis();
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note_name", newNoteName);
        intent.putExtra("is_new", true);
        startActivity(intent);
    }

    private void openNote(String noteName) {
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note_name", noteName);
        intent.putExtra("is_new", false);
        startActivity(intent);
    }

    private void deleteNote(String noteName) {
        File noteFile = new File(notesDirectory, noteName + ".txt");
        if (noteFile.exists()) {
            boolean deleted = noteFile.delete();
            if (deleted) {
                updateStatus("Note deleted: " + noteName);
                loadNotes();
            } else {
                updateStatus("Failed to delete note");
            }
        }
    }

    private void handleSyncButtonClick() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // Request to enable Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // Check Bluetooth permissions for newer Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkBluetoothPermissions();
            } else {
                startBluetoothSync();
            }
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ArrayList<String> permissionsNeeded = new ArrayList<>();

            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }

            if (!permissionsNeeded.isEmpty()) {
                requestPermissions(permissionsNeeded.toArray(new String[0]),
                        REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                startBluetoothSync();
            }
        } else {
            startBluetoothSync();
        }
    }

    private void startBluetoothSync() {
        Intent intent = new Intent(this, BluetoothSyncActivity.class);
        intent.putExtra("notes_directory", notesDirectory.getAbsolutePath());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                updateStatus("Bluetooth enabled");
                btnSync.setText("Sync");
            } else {
                updateStatus("Bluetooth enable cancelled");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    updateStatus("Permissions granted");
                } else {
                    updateStatus("Some permissions denied");
                }
                break;

            case REQUEST_BLUETOOTH_PERMISSIONS:
                boolean bluetoothGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        bluetoothGranted = false;
                        break;
                    }
                }
                if (bluetoothGranted) {
                    startBluetoothSync();
                } else {
                    Toast.makeText(this, "Bluetooth permissions required for sync",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes(); // Refresh notes list when returning from editor
    }

    private void updateStatus(String status) {
        txtStatus.setText(status);
    }

    private void updateNoteCount() {
        int count = notesList.size();
        txtNoteCount.setText(count + (count == 1 ? " note" : " notes"));
    }

    public File getNotesDirectory() {
        return notesDirectory;
    }
}