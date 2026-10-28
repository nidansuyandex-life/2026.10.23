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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化语音引擎
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINA);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("WalkTimerService", "TTS 语言不支持");
                } else {
                    isTtsReady = true;
                    // 如果之前有等待播放的文本，现在播放
                    if (pendingText != null) {
                        tts.speak(pendingText, TextToSpeech.QUEUE_FLUSH, null, "pending");
                        pendingText = null;
                    }
                }
            }
        });
        
        // 前台保活通知
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
            
            // 播放开始语音
            String startMsg = "该站起来走动了，活动两到三分钟";
            if (tts != null) {
                if (isTtsReady) {
                    tts.speak(startMsg, TextToSpeech.QUEUE_FLUSH, null, "start");
                } else {
                    pendingText = startMsg; // 引擎没准备好，先缓存
                }
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
                try {
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000);
                } catch (Exception ignored) {}
                
                // 播放结束语音
                String endMsg = "时间到，请站起来活动一下";
                if (tts != null) {
                    if (isTtsReady) {
                        tts.speak(endMsg, TextToSpeech.QUEUE_FLUSH, null, "end");
                    } else {
                        pendingText = endMsg;
                    }
                }
                
                // 延迟关闭服务，等语音播完
                new Handler(Looper.getMainLooper()).postDelayed(WalkTimerService.this::stopSelf, 5000);
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
