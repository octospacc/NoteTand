package org.eu.octt.notetand;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ReceiveActivity extends CustomActivity {
    BluetoothServerSocket server;
    BluetoothSocket socket;
    TextView textStatus;
    boolean stopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        textStatus = findViewById(R.id.text_status);
        runServer();
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopServer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BluetoothManager.REQUEST_PERMISSION_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                runServer();
            else {
                Toast.makeText(this, "Bluetooth permission not granted! Please retry.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BluetoothManager.REQUEST_ENABLE_BT && resultCode == RESULT_OK)
            runServer();
        else {
            Toast.makeText(this, "Bluetooth permission not granted! Please retry.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    void runServer() {
        if (!BluetoothManager.requireBluetooth(this)) return;
        new Thread(() -> {
            try {
                writeStatus("Initializing Bluetooth server...");
                server = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(getString(R.string.app_name), NoteTand.SERVICE_UUID);

                while (true) {
                    writeStatus("Waiting for new client connection...");
                    socket = server.accept();

                    // if (!socket.isConnected()) return;

                    var client = socket.getRemoteDevice();
                    // textStatus.append("Connected to client!\n");
                    // textStatus.append("Connected: " + client.getName() + " : " + client.getAddress() + '\n');
                    writeStatus("Connected: " + client.getName() + " : " + NoteTand.censorMac(client.getAddress()));

                    // byte[] buffer = new byte[1024];
//                    int bytes;
//
//                    while ((bytes = socket.getInputStream().read(buffer)) != -1) {
//                        var received = new String(buffer, 0, bytes);
//
//                        // runOnUiThread(() -> Toast.makeText(this, "Received: " + received, Toast.LENGTH_SHORT).show());
//                        // textStatus.append(received + '\n');
//                        writeStatus("Receiving data...");
//                        writeStatus("> " + received);
//                    }

                    writeStatus("Reading data...");

                    var titleLength = ByteBuffer.wrap(readFully(4)).getInt(); // ByteBuffer.wrap(intBuffer).order(ByteOrder.BIG_ENDIAN).getInt(); // or LITTLE_ENDIAN
                    var titleBytes = readFully(titleLength);
                    var noteTitle = new String(titleBytes, "UTF-8");

                    var bodyLength = ByteBuffer.wrap(readFully(4)).getInt(); // ByteBuffer.wrap(intBuffer).order(ByteOrder.BIG_ENDIAN).getInt();
                    var bodyBytes = readFully(bodyLength);
                    var noteBody = new String(bodyBytes, "UTF-8");

                    if (titleLength == titleBytes.length && bodyLength == bodyBytes.length) {
                        NoteManager.saveNote(noteTitle, noteBody);
                        writeStatus("Received and saved note (" + titleLength + " + " + bodyLength + " bytes): " + noteTitle);
                    } else {
                        writeStatus("Content length mismatch! Expected " + titleLength + " + " + bodyLength + ", got " + titleBytes.length + " + " + bodyBytes.length);
                    }

                    // writeStatus("Finished receiving!");
                }
            } catch (IOException e) {
                if (!stopped) {
                    writeStatus("Fatal error! Restarting...");
                    e.printStackTrace();
                    runServer();
                }
            }
        }).start();
    }

    void stopServer() {
        stopped = true;
        if (server != null) {
            try {
                if (socket != null) {
                    socket.getInputStream().close();
                    socket.getOutputStream().close();
                    socket.close();
                }
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void writeStatus(String text) {
        runOnUiThread(() -> textStatus.append(text + '\n'));
    }

    byte[] readFully(/* InputStream in, byte[] buffer, */ int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = socket.getInputStream().read(buffer, offset, length - offset);
            if (count == -1) throw new EOFException("Stream ended early");
            offset += count;
        }
        return buffer;
    }
}
