package com.daily.healthlife;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * 站立走动倒计时服务。
 *
 * 稳定性要点（修复「倒计时期间微信来消息闪退」）：
 *  1. onStartCommand 第一行就 startForeground()，且用最小通知、全程 try-catch：
 *     Android 要求 startForegroundService() 之后 5 秒内必须调用 startForeground()，
 *     否则系统会直接杀掉进程并抛 RemoteServiceException；TTS/渠道初始化若卡顿也会触发。
 *  2. 所有系统服务调用（通知、震动、TTS、ToneGenerator）逐一 try-catch：
 *     微信来消息时 App 可能被判为后台 / 通知权限被回收 / 音频焦点被抢，
 *     任一未捕获异常都会让「服务主线程」崩溃，进而整个进程闪退。
 *  3. 服务里的异常绝不允许冒泡到框架。
 */
public class WalkTimerService extends Service {
    private static final String TAG = "WalkTimerService";
    private static final String CHANNEL_ID = "walk_timer_channel_high";
    private static final int NOTIFICATION_ID = 101;
    private static final int ALERT_NOTIFICATION_ID = 102;

    /** 供 JsBridge 判断是否已在跑，避免重复 startForegroundService */
    public static volatile boolean sRunning = false;

    private CountDownTimer timer;
    private TextToSpeech tts;
    private volatile boolean isTtsReady = false;
    private volatile boolean destroyed = false;
    private String pendingText = null;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        // 渠道先建（快），TTS 放后面且全部包 try-catch，避免卡住 startForeground 的 5 秒窗口
        try { createNotificationChannel(); } catch (Throwable t) { Log.e(TAG, "channel failed", t); }
        try {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                try {
                    if (status == TextToSpeech.SUCCESS && tts != null && !destroyed) {
                        int result = tts.setLanguage(Locale.CHINA);
                        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                            isTtsReady = true;
                            if (pendingText != null) {
                                String p = pendingText;
                                pendingText = null;
                                safeSpeak(p, "pending");
                            }
                        }
                    }
                } catch (Throwable t) { Log.e(TAG, "tts init callback failed", t); }
            });
        } catch (Throwable t) {
            Log.e(TAG, "TTS create failed", t);
            tts = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "健康生活提醒", NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000});
            channel.setSound(null, null); // 声音由 ToneGenerator 和 TTS 接管
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sRunning = true;
        // ★ 必须第一件事就做：极简前台通知，确保 5 秒内 startForeground 完成
        ensureForeground();

        try {
            if (intent != null && intent.hasExtra("duration")) {
                int seconds = Math.max(1, intent.getIntExtra("duration", 200));
                final String startMsg = "该站起来走动了，活动两到三分钟";

                // 用完整文案刷新一次通知（失败也不影响前台状态）
                try { updateForegroundNotification("活动倒计时开始", "请起身走动，倒计时 " + seconds + " 秒"); }
                catch (Throwable ignored) {}

                safeSpeak(startMsg, "start");
                startTimer(seconds * 1000L);
            }
        } catch (Throwable t) {
            Log.e(TAG, "onStartCommand failed", t);
        }
        return START_NOT_STICKY;
    }

    /** 极简前台通知：只依赖最基础的系统服务，任何异常都不允许冒泡 */
    private void ensureForeground() {
        try {
            Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("活动倒计时")
                    .setContentText("起身走动中…")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
            startForegroundCompat(n);
        } catch (Throwable t) {
            Log.e(TAG, "startForeground failed, stop service safely", t);
            // 关键：不能让异常冒泡到框架，否则整个进程被杀
            try { stopSelf(); } catch (Throwable ignored) {}
        }
    }

    private void updateForegroundNotification(String title, String content) {
        Notification notification = buildNotification(title, content, true);
        startForegroundCompat(notification);
    }

    private void startForegroundCompat(Notification notification) {
        // Android 14 (targetSdk 34) 要求前台服务类型与 Manifest 声明一致
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String title, String content, boolean ongoing) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void safeSpeak(String text, String utteranceId) {
        try {
            if (text == null || destroyed) return;
            if (tts == null) { pendingText = text; return; }
            if (!isTtsReady) { pendingText = text; return; }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        } catch (Throwable t) { Log.e(TAG, "safeSpeak failed", t); }
    }

    private void startTimer(long ms) {
        try {
            if (timer != null) timer.cancel();
        } catch (Throwable ignored) {}
        try {
            timer = new CountDownTimer(ms, 1000) {
                @Override public void onTick(long millisUntilFinished) {}

                @Override public void onFinish() {
                    if (destroyed) return;
                    // 1. 提示音（用完必须 release，否则多次触发会耗尽资源）
                    try {
                        final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000);
                        mainHandler.postDelayed(() -> { try { toneG.release(); } catch (Throwable ignored) {} }, 2300);
                    } catch (Throwable ignored) {}

                    // 2. 震动
                    try {
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        if (v != null) {
                            if (Build.VERSION.SDK_INT >= 26) {
                                v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                v.vibrate(1000);
                            }
                        }
                    } catch (Throwable ignored) {}

                    // 3. 语音
                    safeSpeak("时间到，请站起来活动一下", "end");

                    // 4. 完成通知（权限可能已被回收，必须包住）
                    try { showAlertNotification("活动结束", "可以回到座位继续工作了"); }
                    catch (Throwable ignored) {}

                    // 5. 延迟收尾
                    try {
                        mainHandler.postDelayed(() -> { try { stopSelf(); } catch (Throwable ignored) {} }, 5000);
                    } catch (Throwable ignored) {}
                }
            }.start();
        } catch (Throwable t) {
            Log.e(TAG, "startTimer failed", t);
            try { stopSelf(); } catch (Throwable ignored) {}
        }
    }

    private boolean canPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showAlertNotification(String title, String content) {
        if (!canPostNotifications()) return;   // Android 13+ 未授权时 nm.notify 会抛 SecurityException
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        manager.notify(ALERT_NOTIFICATION_ID, builder.build());
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        sRunning = false;
        destroyed = true;
        try { mainHandler.removeCallbacksAndMessages(null); } catch (Throwable ignored) {}
        try { if (timer != null) timer.cancel(); } catch (Throwable ignored) {}
        timer = null;
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Throwable ignored) {}
        tts = null;
        try { stopForeground(true); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
