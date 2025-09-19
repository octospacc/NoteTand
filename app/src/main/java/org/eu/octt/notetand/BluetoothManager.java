package org.eu.octt.notetand;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class BluetoothManager {
    static final int REQUEST_ENABLE_BT = 1001;
    static final int REQUEST_PERMISSION_BT = 1002;

    static boolean getPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSION_BT);
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission") // we're checking it but the IDE won't budge
    static boolean requireBluetooth(Activity activity) {
        if (!getPermission(activity))
            return false;
        var adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (adapter.isEnabled()) {
                return true;
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        return false;
    }
}
