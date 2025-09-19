package org.eu.octt.notetand;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothSyncActivity extends Activity {

    private static final String TAG = "BluetoothSync";
    private static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String SERVICE_NAME = "SyncNotesApp";
    private static final int REQUEST_DISCOVERABLE = 2001;
    private static final int DISCOVERABLE_DURATION = 300; // 5 minutes

    // Protocol constants
    private static final String PROTOCOL_HANDSHAKE = "SYNCNOTES_HANDSHAKE";
    private static final String PROTOCOL_FILE_LIST = "FILE_LIST";
    private static final String PROTOCOL_FILE_REQUEST = "FILE_REQUEST";
    private static final String PROTOCOL_FILE_DATA = "FILE_DATA";
    private static final String PROTOCOL_SYNC_COMPLETE = "SYNC_COMPLETE";
    private static final String PROTOCOL_ERROR = "ERROR";

    private BluetoothAdapter bluetoothAdapter;
    private TextView txtBluetoothStatus;
    private TextView txtSyncProgress;
    private Button btnMakeDiscoverable;
    private Button btnScanDevices;
    private Button btnCancelSync;
    private ListView listPairedDevices;
    private ListView listDiscoveredDevices;

    private ArrayAdapter<String> pairedDevicesAdapter;
    private ArrayAdapter<String> discoveredDevicesAdapter;
    private ArrayList<BluetoothDevice> pairedDevicesList;
    private ArrayList<BluetoothDevice> discoveredDevicesList;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private Handler mainHandler;
    private File notesDirectory;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_sync_activity);

        // Enable up navigation
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("Bluetooth Sync");
        }

        mainHandler = new Handler(Looper.getMainLooper());
        initializeViews();
        setupNotesDirectory();
        initializeBluetooth();
        setupListeners();
    }

    private void initializeViews() {
        txtBluetoothStatus = findViewById(R.id.txt_bluetooth_status);
        txtSyncProgress = findViewById(R.id.txt_sync_progress);
        btnMakeDiscoverable = findViewById(R.id.btn_make_discoverable);
        btnScanDevices = findViewById(R.id.btn_scan_devices);
        btnCancelSync = findViewById(R.id.btn_cancel_sync);
        listPairedDevices = findViewById(R.id.list_paired_devices);
        listDiscoveredDevices = findViewById(R.id.list_discovered_devices);

        // Initialize device lists
        pairedDevicesList = new ArrayList<>();
        discoveredDevicesList = new ArrayList<>();

        pairedDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredDevicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        listPairedDevices.setAdapter(pairedDevicesAdapter);
        listDiscoveredDevices.setAdapter(discoveredDevicesAdapter);
    }

    private void setupNotesDirectory() {
        String notesPath = getIntent().getStringExtra("notes_directory");
        if (notesPath != null) {
            notesDirectory = new File(notesPath);
        } else {
            File externalFilesDir = getExternalFilesDir(null);
            if (externalFilesDir != null) {
                notesDirectory = new File(externalFilesDir, "notes");
            } else {
                notesDirectory = new File(getFilesDir(), "notes");
            }
        }
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            updateStatus("Bluetooth not supported on this device");
            disableAllButtons();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            updateStatus("Bluetooth is disabled");
            disableAllButtons();
            return;
        }

        updateStatus("Bluetooth ready");
        loadPairedDevices();
        startAcceptThread();
    }

    private void setupListeners() {
        btnMakeDiscoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeDiscoverable();
            }
        });

        btnScanDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning) {
                    stopDeviceDiscovery();
                } else {
                    startDeviceDiscovery();
                }
            }
        });

        btnCancelSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = pairedDevicesList.get(position);
                connectToDevice(device);
            }
        });

        listDiscoveredDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = discoveredDevicesList.get(position);
                connectToDevice(device);
            }
        });

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);
    }

    private void loadPairedDevices() {
        pairedDevicesList.clear();
        pairedDevicesAdapter.clear();

        if (checkBluetoothPermission()) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    pairedDevicesList.add(device);
                    String deviceInfo = device.getName() + "\n" + device.getAddress();
                    pairedDevicesAdapter.add(deviceInfo);
                }
            } else {
                pairedDevicesAdapter.add("No paired devices found");
            }
        } else {
            pairedDevicesAdapter.add("Bluetooth permission required");
        }

        pairedDevicesAdapter.notifyDataSetChanged();
    }

    private void makeDiscoverable() {
        if (!checkBluetoothPermission()) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
    }

    private void startDeviceDiscovery() {
        if (!checkBluetoothPermission()) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        discoveredDevicesList.clear();
        discoveredDevicesAdapter.clear();
        discoveredDevicesAdapter.notifyDataSetChanged();

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        boolean started = bluetoothAdapter.startDiscovery();
        if (started) {
            isScanning = true;
            btnScanDevices.setText("Stop Scan");
            updateProgress("Scanning for devices...");
        } else {
            Toast.makeText(this, "Failed to start device discovery", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDeviceDiscovery() {
        if (checkBluetoothPermission() && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        isScanning = false;
        btnScanDevices.setText("Scan for Devices");
        updateProgress("");
    }

    private void connectToDevice(final BluetoothDevice device) {
        if (!checkBluetoothPermission()) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop discovery to save resources
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            isScanning = false;
            btnScanDevices.setText("Scan for Devices");
        }

        new AlertDialog.Builder(this)
                .setTitle("Connect to Device")
                .setMessage("Connect to " + device.getName() + " (" + device.getAddress() + ") for sync?")
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startConnectThread(device);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startConnectThread(BluetoothDevice device) {
        // Cancel any existing connection attempts
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        updateProgress("Connecting to " + device.getName() + "...");
    }

    private void startAcceptThread() {
        if (acceptThread != null) {
            acceptThread.cancel();
        }
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void updateStatus(String status) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                txtBluetoothStatus.setText(status);
            }
        });
        Log.d(TAG, "Status: " + status);
    }

    private void updateProgress(String progress) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                txtSyncProgress.setText(progress);
            }
        });
        Log.d(TAG, "Progress: " + progress);
    }

    private void disableAllButtons() {
        btnMakeDiscoverable.setEnabled(false);
        btnScanDevices.setEnabled(false);
    }

    // BroadcastReceiver for Bluetooth device discovery
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && checkBluetoothPermission()) {
                    String deviceName = device.getName();
                    if (deviceName == null) {
                        deviceName = "Unknown Device";
                    }

                    // Avoid duplicates
                    boolean alreadyAdded = false;
                    for (BluetoothDevice existingDevice : discoveredDevicesList) {
                        if (existingDevice.getAddress().equals(device.getAddress())) {
                            alreadyAdded = true;
                            break;
                        }
                    }

                    if (!alreadyAdded) {
                        discoveredDevicesList.add(device);
                        String deviceInfo = deviceName + "\n" + device.getAddress();
                        discoveredDevicesAdapter.add(deviceInfo);
                        discoveredDevicesAdapter.notifyDataSetChanged();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                isScanning = false;
                btnScanDevices.setText("Scan for Devices");
                updateProgress("Discovery finished. Found " + discoveredDevicesList.size() + " devices.");
            }
        }
    };

    // Thread for accepting incoming connections
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            try {
                if (checkBluetoothPermission()) {
                    serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                            SERVICE_NAME, SERVICE_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;

            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                }

                if (socket != null) {
                    // Connection accepted
                    manageConnectedSocket(socket);
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close server socket", e);
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close server socket", e);
            }
        }
    }

    // Thread for connecting to a device
    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            try {
                if (checkBluetoothPermission()) {
                    socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
        }

        @Override
        public void run() {
            // Cancel discovery to save resources
            if (checkBluetoothPermission() && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }

            try {
                if (checkBluetoothPermission()) {
                    socket.connect();
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not connect to device", e);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateProgress("Connection failed: " + e.getMessage());
                        Toast.makeText(BluetoothSyncActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                    }
                });

                try {
                    socket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close client socket", closeException);
                }
                return;
            }

            // Connection successful
            manageConnectedSocket(socket);
        }

        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close client socket", e);
            }
        }
    }

    // Thread for managing a connected socket
    private class ConnectedThread extends Thread {
        private BluetoothSocket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;

            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input/output streams", e);
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    numBytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, numBytes, "UTF-8");
                    handleReceivedMessage(receivedMessage);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateProgress("Connection lost");
                        }
                    });
                    break;
                }
            }
        }

        public void write(String message) {
            try {
                byte[] bytes = message.getBytes("UTF-8");
                outputStream.write(bytes);
                Log.d(TAG, "Sent: " + message);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close connected socket", e);
            }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateProgress("Connected! Starting sync...");
            }
        });

        // Cancel existing threads
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the connected thread
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Start the sync protocol
        startSyncProtocol();
    }

    private void handleReceivedMessage(String message) {
        Log.d(TAG, "Received: " + message);

        String[] parts = message.split("\\|", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case PROTOCOL_HANDSHAKE:
                handleHandshake(data);
                break;
            case PROTOCOL_FILE_LIST:
                handleFileList(data);
                break;
            case PROTOCOL_FILE_REQUEST:
                handleFileRequest(data);
                break;
            case PROTOCOL_FILE_DATA:
                handleFileData(data);
                break;
            case PROTOCOL_SYNC_COMPLETE:
                handleSyncComplete();
                break;
            case PROTOCOL_ERROR:
                handleError(data);
                break;
        }
    }

    private void startSyncProtocol() {
        if (connectedThread != null) {
            connectedThread.write(PROTOCOL_HANDSHAKE + "|" + android.os.Build.MODEL);
        }
    }

    private void handleHandshake(String deviceInfo) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateProgress("Handshake received from " + deviceInfo);
            }
        });

        // Send our file list
        sendFileList();
    }

    private void sendFileList() {
        List<NoteManager.NoteInfo> notes = NoteManager.getAllNoteInfo(notesDirectory);
        StringBuilder fileList = new StringBuilder();

        for (NoteManager.NoteInfo note : notes) {
            if (fileList.length() > 0) {
                fileList.append(";");
            }
            fileList.append(note.name).append(",")
                    .append(note.lastModified).append(",")
                    .append(note.hash);
        }

        if (connectedThread != null) {
            connectedThread.write(PROTOCOL_FILE_LIST + "|" + fileList.toString());
        }
    }

    private void handleFileList(String fileListData) {
        // Parse remote file list and determine what files to sync
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateProgress("Comparing files...");
            }
        });

        // Simple implementation: request all files that are different
        // In a full implementation, you'd compare timestamps and hashes
        String[] files = fileListData.split(";");
        for (String fileInfo : files) {
            if (!fileInfo.isEmpty()) {
                String[] parts = fileInfo.split(",");
                if (parts.length >= 3) {
                    String fileName = parts[0];
                    long timestamp = Long.parseLong(parts[1]);
                    String hash = parts[2];

                    // Check if we need this file
                    NoteManager.NoteInfo localNote = NoteManager.getNoteInfo(notesDirectory, fileName);
                    if (localNote == null || localNote.lastModified < timestamp || !localNote.hash.equals(hash)) {
                        // Request this file
                        if (connectedThread != null) {
                            connectedThread.write(PROTOCOL_FILE_REQUEST + "|" + fileName);
                        }
                    }
                }
            }
        }

        // Send sync complete when done
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (connectedThread != null) {
                    connectedThread.write(PROTOCOL_SYNC_COMPLETE + "|");
                }
            }
        }, 1000);
    }

    private void handleFileRequest(String fileName) {
        String content = NoteManager.loadNote(notesDirectory, fileName);
        if (content != null) {
            if (connectedThread != null) {
                connectedThread.write(PROTOCOL_FILE_DATA + "|" + fileName + ":" + content);
            }
        } else {
            if (connectedThread != null) {
                connectedThread.write(PROTOCOL_ERROR + "|File not found: " + fileName);
            }
        }
    }

    private void handleFileData(String fileData) {
        int colonIndex = fileData.indexOf(':');
        if (colonIndex > 0) {
            String fileName = fileData.substring(0, colonIndex);
            String content = fileData.substring(colonIndex + 1);

            boolean saved = NoteManager.saveNote(notesDirectory, fileName, content);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (saved) {
                        updateProgress("Received file: " + fileName);
                    } else {
                        updateProgress("Failed to save: " + fileName);
                    }
                }
            });
        }
    }

    private void handleSyncComplete() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateProgress("Sync completed successfully!");
                Toast.makeText(BluetoothSyncActivity.this, "Sync completed!", Toast.LENGTH_LONG).show();

                // Close connection after a delay
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 2000);
            }
        });
    }

    private void handleError(String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                updateProgress("Error: " + error);
                Toast.makeText(BluetoothSyncActivity.this, "Sync error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DISCOVERABLE) {
            if (resultCode > 0) {
                updateStatus("Device is discoverable for " + resultCode + " seconds");
                updateProgress("Waiting for incoming connections...");
            } else {
                updateStatus("Discoverable request denied");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up threads
        if (connectThread != null) {
            connectThread.cancel();
        }
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (acceptThread != null) {
            acceptThread.cancel();
        }

        // Unregister broadcast receiver
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }

        // Stop discovery
        if (bluetoothAdapter != null && checkBluetoothPermission() && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }
}