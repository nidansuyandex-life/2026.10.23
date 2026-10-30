package com.daily.healthlife;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WalkTimerService extends Service {
    private CountDownTimer timer;
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private String pendingText = null;
    private static final String CHANNEL_ID = "walk_timer_channel";
    private static final int NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINA);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("WalkTimerService", "TTS 语言不支持");
                } else {
                    isTtsReady = true;
                    if (pendingText != null) {
                        tts.speak(pendingText, TextToSpeech.QUEUE_FLUSH, null, "pending");
                        pendingText = null;
                    }
                }
            }
        });
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "站立倒计时", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("duration")) {
            int seconds = intent.getIntExtra("duration", 200);
            String startMsg = "该站起来走动了，活动两到三分钟";
            
            showNotification("活动倒计时开始", "请起身走动，倒计时 " + seconds + " 秒");
            
            if (tts != null) {
                if (isTtsReady) tts.speak(startMsg, TextToSpeech.QUEUE_FLUSH, null, "start");
                else pendingText = startMsg;
            }
            startTimer(seconds * 1000L);
        }
        return START_NOT_STICKY;
    }

    private void startTimer(long ms) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(ms, 1000) {
            @Override public void onTick(long millisUntilFinished) {}
            @Override public void onFinish() {
                try {
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000);
                } catch (Exception ignored) {}
                
                String endMsg = "时间到，请站起来活动一下";
                if (tts != null) {
                    if (isTtsReady) tts.speak(endMsg, TextToSpeech.QUEUE_FLUSH, null, "end");
                    else pendingText = endMsg;
                }
                
                showNotification("活动结束", "可以回到座位继续工作了");
                
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> stopSelf(), 5000);
            }
        }.start();
    }

    private void showNotification(String title, String content) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
                
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        if (timer != null) timer.cancel();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
