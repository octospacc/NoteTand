package org.eu.octt.notetand;

import java.util.UUID;

public class NoteTand {
    static final UUID SERVICE_UUID = UUID.fromString("fb7befa5-311b-436e-9c2f-9150fe635a40");

    static String censorMac(String mac) {
        if (SettingsManager.getCensorMac()) {
            var parts = mac.split(":");
            return parts[0] + ":••:••:••:••:" + parts[5];
        } else {
            return mac;
        }
    }
}
