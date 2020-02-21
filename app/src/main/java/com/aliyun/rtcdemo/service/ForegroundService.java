package com.aliyun.rtcdemo.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.aliyun.rtcdemo.R;
import com.aliyun.rtcdemo.activity.AliRtcChatActivity;

import static android.support.v4.app.NotificationCompat.FLAG_NO_CLEAR;
import static android.support.v4.app.NotificationCompat.FLAG_ONGOING_EVENT;

public class ForegroundService extends Service {
    private static final int NOTIFICATION_FLAG = 0x11;
    public static final String ID = "0x110088";
    public static final String NAME = ForegroundService.class.getSimpleName();

    public ForegroundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(101, createCompatibleNotification(this));

        }else {
            startForeground(101, createMainNotification(this));

        }


    }

    /**
     * @param context
     * @return
     * 兼容高版本o以上
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification createCompatibleNotification(Context context) {
        NotificationChannel chan = new NotificationChannel(ID, context.getResources().getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager service = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        Intent intentChart = new Intent(this, AliRtcChatActivity.class);

        intentChart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intentChart, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(context, ID)
                .setContentTitle("音视频通话中")
                .setContentText("正在进行音视频通话")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE).build();
    }

    /**
     * @param context
     * @return
     * 兼容低版本
     */
    public Notification createMainNotification(Context context) {
        Intent intentChart = new Intent(this, AliRtcChatActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentChart, 0);
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        builder.setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("音视频通话中")
                .setContentText("正在进行音视频通话")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setPriority(Notification.PRIORITY_HIGH);
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= FLAG_ONGOING_EVENT;
        notification.flags |= FLAG_NO_CLEAR;
        return notification;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }
}
