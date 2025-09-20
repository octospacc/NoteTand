package org.eu.octt.notetand;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SendActivity extends CustomActivity {
    BluetoothSocket socket;
    AlertDialog dialog;
    String statusLog;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        setActionBarBack();
        showTargets();
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BluetoothManager.REQUEST_PERMISSION_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                showTargets();
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
            showTargets();
        else {
            Toast.makeText(this, "Bluetooth permission not granted! Please retry.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    void showTargets() {
        if (!BluetoothManager.requireBluetooth(this)) return;

        ListView listPairedDevices = findViewById(R.id.list_paired_devices);
        var pairedDevicesList = new ArrayList<BluetoothDevice>();
        var pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listPairedDevices.setAdapter(pairedDevicesAdapter);

        var pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            pairedDevicesList.add(device);
            pairedDevicesAdapter.add(device.getName() + '\n' + NoteTand.censorMac(device.getAddress()));
        }
        pairedDevicesAdapter.notifyDataSetChanged();

        listPairedDevices.setOnItemClickListener((parent, view, position, id) -> {
            statusLog = "";
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.sending_note)
                    .setMessage("Preparing...")
                    // .setNegativeButton("Abort", null)
                    .setNeutralButton(R.string.close, null)
                    .show();
            setDialogCancelable(false);

            var device = pairedDevicesList.get(position);
            new Thread(() -> {
                try {
                    writeStatus("Creating socket to target device...");
                    socket = device.createRfcommSocketToServiceRecord(NoteTand.SERVICE_UUID);

                    writeStatus("Trying to connect...");
                    socket.connect();
                    writeStatus("Connected successfully!");
                    Log.d("Bluetooth", "Connection successful");
                    var output = socket.getOutputStream();

                    var noteName = getIntent().getStringExtra("note");
                    var noteContent = NoteManager.loadNote(noteName);

                    var nameBytes = noteName.getBytes("UTF-8");
                    var contentBytes = noteContent.getBytes("UTF-8");

                    writeStatus("Sending data...");

                    output.write(ByteBuffer.allocate(4).putInt(nameBytes.length /*noteName.length()*/).array());
                    output.write(nameBytes);

                    output.write(ByteBuffer.allocate(4).putInt(contentBytes.length /*noteContent.length()*/).array());
                    output.write(contentBytes);

                    writeStatus("Note sent!");
                    setDialogCancelable(true);

                    socket.getInputStream().close();
                    socket.getOutputStream().close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Bluetooth", "Connection failed", e);
                    writeStatus(e.getMessage());
                    setDialogCancelable(true);
                }
                closeSocket();
            }).start();
        });
    }

    void closeSocket() {
        if (socket != null) {
            try {
                socket.getInputStream().close();
                socket.getOutputStream().close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void writeStatus(String text) {
        statusLog = statusLog + text + '\n';
        runOnUiThread(() -> dialog.setMessage(statusLog.trim()));
    }

    void setDialogCancelable(boolean status) {
        runOnUiThread(() -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(status);
            dialog.setCancelable(status);
        });
    }
}
