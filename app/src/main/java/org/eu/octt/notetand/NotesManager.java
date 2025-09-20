package org.eu.octt.notetand;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NotesManager {
    private static final String TAG = "NoteManager";

    public static File externalNotesDirectory;
    public static File internalNotesDirectory;

    public static class NoteInfo {
        public String name;
        public String content;
        public long lastModified;
        public String hash;
        public long size;

        public NoteInfo(String name, String content, long lastModified) {
            this.name = name;
            this.content = content;
            this.lastModified = lastModified;
            this.size = content.getBytes().length;
            this.hash = calculateHash(content);
        }
    }

    static File getNotesDirectory() {
        switch (SettingsManager.getDefaultLocation()) {
            case "private_external":
                return externalNotesDirectory;
            case "private_internal":
                return internalNotesDirectory;
        }
        return null;
    }

    /**
     * Save a note to the specified directory
     */
    public static boolean saveNote(File notesDir, String noteName, String content) {
        try {
            File noteFile = new File(notesDir, noteName /* + ".txt" */);
            FileOutputStream fos = new FileOutputStream(noteFile);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            Log.d(TAG, "Note saved: " + noteName);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving note: " + noteName, e);
            return false;
        }
    }

    public static boolean saveNote(String noteName, String content) {
        try {
            File noteFile = new File(getNotesDirectory(), noteName /* + ".txt" */);
            FileOutputStream fos = new FileOutputStream(noteFile);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
            Log.d(TAG, "Note saved: " + noteName);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error saving note: " + noteName, e);
            return false;
        }
    }

    /**
     * Load a note from the specified directory
     */
    public static String loadNote(File notesDir, String noteName) {
        try {
            File noteFile = new File(notesDir, noteName /* + ".txt" */);
            if (!noteFile.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(noteFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();
            fis.close();

            // Remove last newline if present
            if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
                content.deleteCharAt(content.length() - 1);
            }

            return content.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error loading note: " + noteName, e);
            return null;
        }
    }

    public static String loadNote(String noteName) {
        try {
            File noteFile = new File(getNotesDirectory(), noteName);
            if (!noteFile.exists()) {
                return null;
            }
            FileInputStream fis = new FileInputStream(noteFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            fis.close();
            // Remove last newline if present
            if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
                content.deleteCharAt(content.length() - 1);
            }
            return content.toString();
        } catch (IOException e) {
            Log.e(TAG, "Error loading note: " + noteName, e);
            return null;
        }
    }

    /**
     * Delete a note from the specified directory
     */
    public static boolean deleteNote(File notesDir, String noteName) {
        File noteFile = new File(notesDir, noteName /* + ".txt" */);
        if (noteFile.exists()) {
            boolean deleted = noteFile.delete();
            if (deleted) {
                Log.d(TAG, "Note deleted: " + noteName);
            } else {
                Log.e(TAG, "Failed to delete note: " + noteName);
            }
            return deleted;
        }
        return false;
    }

    /**
     * Get all notes from the specified directory
     */
//    public static List<String> getAllNoteNames(File notesDir) {
//        List<String> noteNames = new ArrayList<>();
//
//        if (notesDir != null && notesDir.exists()) {
//            File[] files = notesDir.listFiles();
//            if (files != null) {
//                for (File file : files) {
//                    if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
//                        String noteName = file.getName();//.replace(".txt", "");
//                        noteNames.add(noteName);
//                    }
//                }
//            }
//        }
//
//        return noteNames;
//    }

    public static List<String> getAllNoteNames() {
        var noteNames = new ArrayList<String>();
        var files = getNotesDirectory().listFiles();
        if (files != null) {
            switch (SettingsManager.getSortingMode()) {
                case "filename":
                    Arrays.sort(files, (o1, o2) -> {
                        // implemented manually for old API compatibility
                        {} // put this empty block to avoid the IDE complaining of that...
                        return o1.getName().compareTo(o2.getName());
                    });
                    break;
                case "modification":
                    Arrays.sort(files, (o1, o2) -> {
                        var diff = o1.lastModified() - o2.lastModified();
                        return diff < 0 ? -1 : (diff > 0 ? 1 : 0);
                    });
                    break;
            }
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                    noteNames.add(file.getName());
                }
            }
        }
        Collections.reverse(noteNames);
        return noteNames;
    }

    /**
     * Get detailed information about all notes
     */
    public static List<NoteInfo> getAllNoteInfo(File notesDir) {
        List<NoteInfo> noteInfoList = new ArrayList<>();

        if (notesDir != null && notesDir.exists()) {
            File[] files = notesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".txt")) {
                        String noteName = file.getName();//.replace(".txt", "");
                        String content = loadNote(notesDir, noteName);
                        if (content != null) {
                            NoteInfo info = new NoteInfo(noteName, content, file.lastModified());
                            noteInfoList.add(info);
                        }
                    }
                }
            }
        }

        return noteInfoList;
    }

    /**
     * Get information about a specific note
     */
    public static NoteInfo getNoteInfo(File notesDir, String noteName) {
        File noteFile = new File(notesDir, noteName /* + ".txt" */);
        if (!noteFile.exists()) {
            return null;
        }

        String content = loadNote(notesDir, noteName);
        if (content != null) {
            return new NoteInfo(noteName, content, noteFile.lastModified());
        }

        return null;
    }

    /**
     * Calculate SHA-256 hash of content for sync comparison
     */
    public static String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "Error calculating hash", e);
            return String.valueOf(content.hashCode()); // Fallback to simple hash
        }
    }

    /**
     * Check if a note exists
     */
    public static boolean noteExists(File notesDir, String noteName) {
        File noteFile = new File(notesDir, noteName /* + ".txt" */);
        return noteFile.exists();
    }

    /**
     * Get the size of notes directory
     */
    public static long getDirectorySize(File notesDir) {
        long size = 0;
        if (notesDir != null && notesDir.exists()) {
            File[] files = notesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }

    /**
     * Validate note name (no special characters that could cause file system issues)
     */
    public static boolean isValidNoteName(String noteName) {
        if (noteName == null || noteName.trim().isEmpty()) {
            return false;
        }

        // Check for invalid characters
        String invalidChars = "\\/:*?\"<>|";
        for (char c : invalidChars.toCharArray()) {
            if (noteName.indexOf(c) != -1) {
                return false;
            }
        }

        // Check length (filesystem limitations)
        return noteName.length() <= 100;
    }

    /**
     * Sanitize note name for file system compatibility
     */
    public static String sanitizeNoteName(String noteName) {
        if (noteName == null) {
            return "untitled";
        }

        // Replace invalid characters with underscores
        String sanitized = noteName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // Trim and limit length
        sanitized = sanitized.trim();
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        // Ensure it's not empty
        if (sanitized.isEmpty()) {
            sanitized = "untitled";
        }

        return sanitized;
    }

    public static void setup(Context context) {
        if (internalNotesDirectory == null) {
            internalNotesDirectory = new File(context.getFilesDir(), "notes");
            if (!internalNotesDirectory.exists())
                internalNotesDirectory.mkdirs();
        }
        if (externalNotesDirectory == null) {
            externalNotesDirectory = new File(context.getExternalFilesDir(null), "notes");
            if (!externalNotesDirectory.exists())
                externalNotesDirectory.mkdirs();
            // if (!externalNotesDirectory.exists() && internalNotesDirectory.exists())
            //     SettingsManager.prefs.edit().putString("default_location", "private_internal").apply();
        }
    }
}
