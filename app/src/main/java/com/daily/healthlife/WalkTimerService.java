package com.daily.healthlife;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class WalkTimerService extends Service {
    private CountDownTimer timer;
    private TextToSpeech tts;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化原生语音引擎
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.CHINA);
        });
        
        // 开启前台通知，保活服务
        String channelId = "walk_timer";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "站立倒计时", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("活动倒计时运行中")
                .setContentText("倒计时结束后会提醒你")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("duration")) {
            int seconds = intent.getIntExtra("duration", 200);
            
            // ★ 倒计时开始时：原生语音播报 + 震动
            if (tts != null) {
                tts.speak("该站起来走动了，活动两到三分钟", TextToSpeech.QUEUE_FLUSH, null, "start");
            }
            
            startTimer(seconds * 1000L);
        }
        return START_NOT_STICKY;
    }

    private void startTimer(long ms) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}
            
            @Override
            public void onFinish() {
                // ★ 倒计时结束时：播放系统提示音 + 语音播报
                try {
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000);
                } catch (Exception ignored) {}
                
                if (tts != null) {
                    tts.speak("时间到，请站起来活动一下", TextToSpeech.QUEUE_FLUSH, null, "end");
                }
                
                // 播放完后延迟销毁服务
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    stopSelf();
                }, 5000);
            }
        }.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
