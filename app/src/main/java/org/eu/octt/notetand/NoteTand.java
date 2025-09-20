package org.eu.octt.notetand;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
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

    @SuppressLint("MissingPermission")
    static String getBluetoothName(BluetoothDevice device) {
        var name = device.getName();
        return (name != null && !name.isEmpty() ? name : "<?>");
    }

    static void sendNotification(Context context, String title, String message, String channelId, String channelName) {
        Notification.Builder builder = null;
        Notification notification;
        var notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        // var contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        // Create NotificationChannel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, channelId);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new Notification.Builder(context);
        }

        if (builder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.ic_dialog_email)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
//                .setContentIntent(contentIntent)
//                .setAutoCancel(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                builder
                    .setStyle(new Notification.BigTextStyle().bigText(message))
                    .setPriority(Notification.PRIORITY_LOW);
            notification = builder.getNotification();
        } else {
            notification = new Notification();
            notification.icon = android.R.drawable.ic_dialog_email;
            notification.when = System.currentTimeMillis();
            try {
                // we need reflection because Google removed the method from the entire SDK...
                Notification.class.getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class)
                        // .invoke(notification, this, title, message, contentIntent);
                        .invoke(notification, context, title, message, PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        notification.sound = null;
        notification.vibrate = null;
        notification.defaults = 0;
        notificationManager.notify(UUID.randomUUID().hashCode(), notification);
    }
}
