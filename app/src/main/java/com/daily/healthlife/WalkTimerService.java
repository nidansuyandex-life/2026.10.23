package com.daily.healthlife;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WalkTimerService extends Service {

    private static final String CHANNEL_ID = "walk_timer_channel";
    private static final int NOTIFICATION_ID = 2001;
    private CountDownTimer countDownTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int duration = 200; // 默认 200 秒
        if (intent != null && intent.hasExtra("duration")) {
            duration = intent.getIntExtra("duration", 200);
        }

        startForeground(NOTIFICATION_ID, buildNotification(duration));

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // 开启倒计时
        countDownTimer = new CountDownTimer(duration * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateNotification((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                showFinishNotification();
                stopSelf();
            }
        }.start();

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "走动倒计时服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(int seconds) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                // ★ 注意这里：把 R.mipmap 改成了 R.drawable
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("该站起来走动了")
                .setContentText("还剩 " + seconds + " 秒，起来活动一下吧")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(int seconds) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(seconds));
        }
    }

    private void showFinishNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    // ★ 注意这里：把 R.mipmap 改成了 R.drawable
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("活动结束")
                    .setContentText("可以回到座位继续工作了")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            manager.notify(NOTIFICATION_ID + 1, builder.build());
        }
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
