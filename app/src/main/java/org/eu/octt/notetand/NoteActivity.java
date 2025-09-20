package org.eu.octt.notetand;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class NoteActivity extends CustomActivity {
    static final int REQUEST_PERMISSION_NOTIFICATIONS = 1011;
    static final String[] EMOJIS = {"🐕", "🐶", "🐩", "🐈", "🐱", "🐀", "🐁", "🐭", "🐹", "🐢", "🐇", "🐰", "🐓", "🐔", "🐣", "🐤", "🐥", "🐦", "🐏", "🐑", "🐐", "🐺", "🐃", "🐂", "🐄", "🐮", "🐴", "🐗", "🐖", "🐷", "🐽", "🐸", "🐍", "🐼", "🐧", "🐘", "🐨", "🐒", "🐵", "🐆", "🐯", "🐻", "🐫", "🐪", "🐊", "🐳", "🐋", "🐟", "🐠", "🐡", "🐙", "🐚", "🐬", "🐌", "🐛", "🐜", "🐝", "🐞", "🐲", "🐉", "🐾", "👻", "👹", "👺", "👽", "👾", "👿", "💀", "💖", "💗", "💘", "💝", "💞", "💟", "🍙", "🍘", "🍠", "🍌", "🍎", "🍏", "🍊", "🍋", "🍄", "🍅", "🍆", "🍇", "🍈", "🍉", "🍐", "🍑", "🍒", "🍓", "🍍", "🌰", "🌱", "🌲", "🌳", "🌴", "🌵", "🌷", "🌸", "🌹", "🍀", "🍁", "🍂", "🍃", "🌺", "🌻", "🌼", "🌽", "🌾", "🌿"};

    private EditText editNoteName;
    private EditText editNoteContent;
    private WebView webView;
    private TextView txtNoteInfo;

    private String originalNoteName;
    private String originalContent;
    private boolean isNewNote;
    private boolean hasUnsavedChanges;
    private AlertDialog dialog;
    private String statusLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SettingsManager.getBlockCapture())
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_note);
        setActionBarBack();
        var useWebview = SettingsManager.getTextEditor().equals("web_textarea") && !SettingsManager.getKeyboardMode().equals("privacy");

        NotesManager.setup(this);
        initializeViews(useWebview);
        loadNoteFromIntent();
        setupListeners();
        updateNoteInfo();

        if (useWebview) {
            var keyboardMode = SettingsManager.getKeyboardMode();
            var fontSize = SettingsManager.getFontSize();
            var cssFontSize = fontSize > 0 ? "font-size: " + fontSize + "px;" : "";
            webView.loadDataWithBaseURL(null, "<!DOCTYPE html><html>" +
                "<head><meta charset='utf8' />" +
                "<style>" +
                    "html, body, textarea { width: 100%; height: 100%; margin: 0; padding: 0; border: none; outline: none; background-color: transparent; color: " + getTextColor() + " }" +
                    "html, body { overflow: hidden; }" +
                    "textarea { overflow-y: scroll; " + cssFontSize + "font-family: " + SettingsManager.getFontType().replace('_', '-') + "; }" +
                "</style>" +
                "</head><body><textarea autocomplete='" + (Arrays.asList("autocorrect", "normal").contains(keyboardMode) ? "on" : "off") + "' spellcheck='" + (keyboardMode.equals("autocorrect") ? "true" : "false") + "'>" + escapeHtml(originalContent) + "</textarea><script>" +
                    "document.getElementsByTagName('textarea')[0].oninput = function(ev){ NoteTand.sendText(ev.target.value); };" +
                "</script></body></html>", "text/html", "utf8", null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                sendNoteNotifications();
            else
                Toast.makeText(this, R.string.notifications_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void initializeViews(boolean useWebview) {
        editNoteName = findViewById(R.id.edit_note_name);
        editNoteContent = findViewById(R.id.edit_note_content);
        txtNoteInfo = findViewById(R.id.txt_note_info);
        webView = findViewById(R.id.webview);

        if (useWebview) {
            editNoteContent.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            webView.setBackgroundColor(Color.TRANSPARENT);

            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new Object(){
                @JavascriptInterface
                public void sendText(String text) {
                    runOnUiThread(() -> editNoteContent.setText(text));
                }
            }, "NoteTand");
        }

        var fontSize = SettingsManager.getFontSize();
        if (fontSize > 0)
            editNoteContent.setTextSize(fontSize);

        Integer flag = null;
        var contentFlags = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        switch (SettingsManager.getKeyboardMode()) {
            case "normal":
                editNoteContent.setInputType(contentFlags);
                break;
            case "no_suggestions":
                flag = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
                break;
            case "privacy":
                flag = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                break;
        }
        if (flag != null) {
            editNoteContent.setInputType(contentFlags | flag);
            editNoteName.setInputType(InputType.TYPE_CLASS_TEXT | flag);
        }

        Typeface typeface = null;
        switch (SettingsManager.getFontType()) {
            case "sans_serif":
                typeface = Typeface.SANS_SERIF;
                break;
            case "serif":
                typeface = Typeface.SERIF;
                break;
            case "monospace":
                typeface = Typeface.MONOSPACE;
                break;
        }
        if (typeface != null) {
            editNoteContent.setTypeface(typeface);
            editNoteName.setTypeface(typeface);
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void loadNoteFromIntent() {
        Intent intent = getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            originalContent = intent.getStringExtra(Intent.EXTRA_TEXT);
        } else {
            originalContent = intent.getStringExtra("content");
            originalNoteName = intent.getStringExtra("note_name");
            isNewNote = intent.getBooleanExtra("is_new", false);
        }

        if (originalNoteName == null) {
            originalNoteName = (new SimpleDateFormat("yyyy-MM-dd HH-mm-ss.SSS")).format(new Date()) + ' ' + EMOJIS[(new Random()).nextInt(EMOJIS.length)] + ".txt";
            isNewNote = true;
        }

        editNoteName.setText(originalNoteName);

        if (!isNewNote || originalContent != null) {
            // Load existing note content
            if (originalNoteName != null && originalContent == null)
                originalContent = NotesManager.loadNote(NotesManager.getNotesDirectory(), originalNoteName);
            if (originalContent != null) {
                editNoteContent.setText(originalContent);
                editNoteContent.setSelection(originalContent.length()); // Move cursor to end
            } else {
                originalContent = "";
                Toast.makeText(this, R.string.error_loading_note, Toast.LENGTH_SHORT).show();
            }
        }

        if (originalContent == null) {
            originalContent = "";
        }

        hasUnsavedChanges = (isNewNote && !originalContent.isEmpty()); // false;
        if (hasUnsavedChanges)
            setTitle("* " + getString(R.string.note));
    }

    private void setupListeners() {
        // Track changes to note name
        editNoteName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Track changes to note content
        editNoteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
                updateNoteInfo();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkForChanges() {
        String currentName = editNoteName.getText().toString().trim();
        String currentContent = editNoteContent.getText().toString();

        hasUnsavedChanges = !currentName.equals(originalNoteName) || !currentContent.equals(originalContent);

        // Update title to indicate unsaved changes
        setTitle((hasUnsavedChanges ? "* " : "") + getString(R.string.note));
    }

    private void updateNoteInfo() {
        String content = editNoteContent.getText().toString();
        int characterCount = content.length();
        int wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        int lineCount = content.split("\n").length;

        if (isNewNote && !hasUnsavedChanges) {
            txtNoteInfo.setText(R.string.new_note);
        } else {
            txtNoteInfo.setText(getString(R.string.note_info_bar, characterCount, wordCount, lineCount));
        }
    }

    private void saveNote() {
        String noteName = editNoteName.getText().toString().trim();

        // Validate note name
        if (noteName.isEmpty()) {
            editNoteName.setError("Note name cannot be empty");
            editNoteName.requestFocus();
            return;
        }

        // Sanitize note name
        String sanitizedName = NotesManager.sanitizeNoteName(noteName);
        if (!sanitizedName.equals(noteName)) {
            editNoteName.setText(sanitizedName);
            noteName = sanitizedName;
            Toast.makeText(this, "Note name was sanitized for compatibility", Toast.LENGTH_SHORT).show();
        }

        // Check if we're renaming and the new name already exists
        if (!noteName.equals(originalNoteName) && NotesManager.noteExists(NotesManager.getNotesDirectory(), noteName)) {
            new AlertDialog.Builder(this)
                .setTitle("Note Already Exists")
                .setMessage("A note with the name '" + noteName + "' already exists. Do you want to overwrite it?")
                .setPositiveButton("Overwrite", (dialog, which) -> performSave())
                .setNeutralButton("Cancel", null)
                .show();
        } else {
            performSave();
        }
    }

    private void performSave() {
        String noteName = editNoteName.getText().toString().trim();
        String content = editNoteContent.getText().toString();

        // Delete old note if we're renaming
        if (!isNewNote && !noteName.equals(originalNoteName)) {
            NotesManager.deleteNote(NotesManager.getNotesDirectory(), originalNoteName);
        }

        // Save the note
        boolean success = NotesManager.saveNote(NotesManager.getNotesDirectory(), noteName, content);

        if (success) {
            originalNoteName = noteName;
            originalContent = content;
            isNewNote = false;
            hasUnsavedChanges = false;

            setTitle(getString(R.string.note));
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
            updateNoteInfo();
        } else {
            Toast.makeText(this, R.string.error_saving_note, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteNote() {
        if (isNewNote) {
            // Just finish if it's a new unsaved note
            finish();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_note)
            .setMessage(getString(R.string.delete_note_message, originalNoteName))
            .setPositiveButton(R.string.delete, (dialog, which) -> deleteNote())
            .setNeutralButton(R.string.cancel, null)
            .show();
    }

    private void deleteNote() {
        boolean success = NotesManager.deleteNote(NotesManager.getNotesDirectory(), originalNoteName);
        if (success) {
            Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.error_deleting_note, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        var id = item.getItemId();
        if (id == R.id.action_save) {
            saveNote();
        } else if (id == R.id.action_delete) {
            confirmDeleteNote();
        } else if (id == R.id.action_send) {
            startActivity(new Intent(this, SendActivity.class).putExtra("note", originalNoteName));
        } else if (id == R.id.action_send_notifications) {
            sendNoteNotifications();
        } else if (id == R.id.action_share) {
            startActivity(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, originalContent));
        } else if (id == android.R.id.home) {
            onBackPressed();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.unsaved_changes)
                .setMessage(R.string.unsaved_changes_message)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    saveNote();
                    if (!hasUnsavedChanges) { // Only finish if save was successful
                        finish();
                    }
                })
                .setNegativeButton(R.string.discard, (dialog, which) -> finish())
                .setNeutralButton(R.string.cancel, null)
                .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle Ctrl+S for save (for external keyboards)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && keyCode == KeyEvent.KEYCODE_S && event.isCtrlPressed()) {
            saveNote();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void sendNoteNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSION_NOTIFICATIONS);
            return;
        }
        final var content = originalContent.trim();
        if (content.isEmpty())
            return;
        final var lengthLimit = SettingsManager.getNotificationsLength();
        final var limit = lengthLimit - 6; // leave margin for ellipsis
        final var cooldown = SettingsManager.getNotificationsCooldown();
        final var aborted = new AtomicBoolean(false);
        statusLog = getString(R.string.sending_note_notifications, lengthLimit, cooldown) + '\n';
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.sending_note)
                .setMessage(statusLog)
                .setNegativeButton(R.string.abort, (dialog1, which) -> aborted.set(true))
                .setNeutralButton("Background", null)
                .setCancelable(true)
                .show();
        new Thread(() -> {
            var length = content.length();
            var prefix = "";
            var count = 1;
            for (int i=0; i<length; i+= limit) {
                if (aborted.get())
                    break;
                var end = Math.min(length, i + limit);
                var isNotLast = end < length;
                var segment = content.substring(i, end);
                if (isNotLast)
                    segment += "...";
                NoteTand.sendNotification(this, "(" + count + ") " + originalNoteName, prefix + segment, "NOTES_CHANNEL", getString(R.string.notes));
                writeStatus(prefix + count);
                prefix = "...";
                count++;
                if (isNotLast) {
                    try {
                        Thread.sleep(cooldown);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            writeStatus(".\n" + getString(R.string.notifications_complete));
            runOnUiThread(() -> {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(TextView.GONE);
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText(R.string.close);
            });
        }).start();
    }

    private void writeStatus(String text) {
        runOnUiThread(() -> dialog.setMessage((statusLog = statusLog + text).trim()));
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getTextColor() {
        var typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        return String.format("#%06X", (0xFFFFFF & getResources().getColor(typedValue.resourceId)));
    }
}
