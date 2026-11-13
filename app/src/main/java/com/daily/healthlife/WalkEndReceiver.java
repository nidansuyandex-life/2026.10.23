package com.daily.healthlife;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * 走动倒计时「时间到」的兜底提醒。
 *
 * WalkTimerService 是前台服务，但国产 ROM（小米/华为/OPPO 等）常把后台服务清掉，
 * 导致到点不响。这里用一条精确闹钟在「到点时刻」直接弹通知 + 震动，
 * 即便服务已被杀，用户也一定收到「可以回座位继续工作」的提醒。
 *
 * 闹钟由 WalkTimerService 在安全结束时取消，正常流程下不会重复提醒。
 */
public class WalkEndReceiver extends BroadcastReceiver {

    public static final String ACTION_END = "com.daily.healthlife.WALK_END";
    private static final int REQ_END = 4303;
    private static final String CHANNEL = "walk_timer_channel_high";

    /** 排一条精确闹钟，在 seconds 秒后触发「时间到」提醒 */
    public static void schedule(Context ctx, int seconds) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = pending(ctx);
            long at = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                exact = false;
            }
            try {
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (SecurityException se) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Exception ignored) {
        }
    }

    /** 取消兜底闹钟（倒计时正常结束或服务销毁时调用） */
    public static void cancel(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pending(ctx));
        } catch (Exception ignored) {
        }
    }

    private static PendingIntent pending(Context ctx) {
        Intent i = new Intent(ctx, WalkEndReceiver.class).setAction(ACTION_END);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQ_END, i, flags);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ctx == null) return;
        showAlert(ctx);
        vibrate(ctx);
    }

    private void showAlert(Context ctx) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) return;
            }
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "健康生活提醒", NotificationManager.IMPORTANCE_HIGH);
                ch.enableVibration(true);
                ch.setSound(null, null); // 震动由下方 Vibrator 接管
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(ctx, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent content = PendingIntent.getActivity(ctx, 4304, open, flags);

            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("活动结束")
                    .setContentText("可以回到座位继续工作了")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 400, 180, 400})
                    .setContentIntent(content);
            nm.notify(102, b.build());
        } catch (Exception ignored) {
        }
    }

    private void vibrate(Context ctx) {
        try {
            Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(new long[]{0, 400, 180, 400}, -1));
            } else {
                v.vibrate(new long[]{0, 400, 180, 400}, -1);
            }
        } catch (Exception ignored) {
        }
    }
}
