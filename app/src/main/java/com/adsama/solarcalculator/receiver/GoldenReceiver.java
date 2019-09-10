package com.adsama.solarcalculator.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.adsama.solarcalculator.MainActivity;
import com.adsama.solarcalculator.R;

public class GoldenReceiver extends BroadcastReceiver {

    private static final String APP_CHANNEL_ID = "911";
    private static final int APP_NOTIFICATION_ID = 47;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equalsIgnoreCase("golden_hour_action")) {
            generateAppNotification(context);
        }
    }

    private void generateAppNotification(Context ctx) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx, APP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(ctx.getString(R.string.golden_hour_begins))
                .setContentText(ctx.getString(R.string.golden_hour_body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(APP_CHANNEL_ID, ctx.getString(R.string.golden_hour_begins), NotificationManager.IMPORTANCE_DEFAULT);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                notificationManager.notify(APP_NOTIFICATION_ID, notificationBuilder.build());
            }
        }
    }

}