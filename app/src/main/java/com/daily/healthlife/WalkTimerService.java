package com.daily.healthlife;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class WalkTimerService extends Service {
    private CountDownTimer timer;
    private final String CHANNEL_ID = "WalkTimerChannel";
    private final int NOTIFY_ID = 2001;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int duration = intent != null ? intent.getIntExtra("duration", 200) : 200;
        createNotificationChannel();

        // 启动前台服务，防止被系统杀后台
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("日常·健康生活")
                .setContentText("正在后台为您倒数 " + duration + " 秒...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build();
        startForeground(NOTIFY_ID, notification);

        if (timer != null) timer.cancel();
        timer = new CountDownTimer(duration * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                Notification updateNotify = new NotificationCompat.Builder(WalkTimerService.this, CHANNEL_ID)
                        .setContentTitle("日常·健康生活")
                        .setContentText("距离活动提醒还剩 " + secondsLeft + " 秒...")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true)
                        .build();
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.notify(NOTIFY_ID, updateNotify);
            }

            @Override
            public void onFinish() {
                showFinishNotification();
                stopSelf();
            }
        }.start();

        return START_NOT_STICKY;
    }

    private void showFinishNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("该站起来走动了！")
                .setContentText("起身走动 2-3 分钟，下巴轻收 5 次")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // 默认震动和声音
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFY_ID + 1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "久坐提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于后台倒计时和结束提醒");
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
