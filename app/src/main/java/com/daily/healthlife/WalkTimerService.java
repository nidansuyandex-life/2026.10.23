package com.daily.healthlife;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
    private static final String CHANNEL_ID = "walk_timer_channel_high";
    private static final int NOTIFICATION_ID = 101;
    private static final int ALERT_NOTIFICATION_ID = 102;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 TTS
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
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "健康生活提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000});
            channel.setSound(null, null); // 声音由 ToneGenerator 和 TTS 接管，防止冲突
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("duration")) {
            int seconds = intent.getIntExtra("duration", 200);
            String startMsg = "该站起来走动了，活动两到三分钟";
            
            // 显示持续倒计时的前台通知
            showForegroundNotification("活动倒计时开始", "请起身走动，倒计时 " + seconds + " 秒");
            
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
                // 1. 播放警报音
                try {
                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000);
                } catch (Exception ignored) {}
                
                // 2. 震动（非常重要，确保后台能感知）
                try {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        if (Build.VERSION.SDK_INT >= 26) {
                            v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            v.vibrate(1000);
                        }
                    }
                } catch (Exception ignored) {}
                
                // 3. TTS 语音播报
                String endMsg = "时间到，请站起来活动一下";
                if (tts != null) {
                    if (isTtsReady) tts.speak(endMsg, TextToSpeech.QUEUE_FLUSH, null, "end");
                    else pendingText = endMsg;
                }
                
                // 4. 弹出高优先级通知（即使 App 在后台或被杀死，只要服务存活就会弹）
                showAlertNotification("活动结束", "可以回到座位继续工作了");
                
                // 5. 延迟 5 秒后停止服务
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> stopSelf(), 5000);
            }
        }.start();
    }

    private void showForegroundNotification(String title, String content) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true) // 常驻，不可滑动删除
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
                
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void showAlertNotification(String title, String content) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL); // 启用默认震动和声音

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(ALERT_NOTIFICATION_ID, builder.build());
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
    
    @Override public void onDestroy() {
        if (timer != null) timer.cancel();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
