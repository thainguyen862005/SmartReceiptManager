package com.example.smartreceiptmanager.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.smartreceiptmanager.MainActivity;

public class NotificationHelper {

    private static final String CHANNEL_ID = "smart_receipt_notifications";
    private static final String CHANNEL_NAME = "Smart Receipt Alerts";

    public static void showNotification(Context context, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        manager.notify((int) System.currentTimeMillis(), builder.build());
        saveNotification(context, title + ": " + message);
    }

    public static void saveNotification(Context context, String text) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("notifications_history", Context.MODE_PRIVATE);
        String currentList = prefs.getString("list", "");
        String newList = text + "##" + currentList;
        prefs.edit().putString("list", newList).apply();
    }

    public static String[] getNotifications(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("notifications_history", Context.MODE_PRIVATE);
        String raw = prefs.getString("list", "");
        if (raw.isEmpty()) {
            return new String[]{"Không có thông báo mới."};
        }
        return raw.split("##");
    }
}
