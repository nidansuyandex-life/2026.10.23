package com.daily.healthlife;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * 手动待办「到点提醒」原生调度器。
 *
 * 之前手动待办只记了时间，却不会到点推送提醒。这里让页面把每条未过期、未完成的
 * 待办同步过来，用 AlarmManager 在系统层面排精确闹钟；App 关掉、后台冻结也能准点触发。
 * 与站立提醒不同：每条待办一个独立闹钟（requestCode 由 date+time+title 派生），
 * 删除 / 完成 / 过期时由页面调用 cancel() 撤销。
 */
public class TodoAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_FIRE = "com.daily.healthlife.TODO_FIRE";
    private static final String CHANNEL = "todo_reminder";
    private static final int REQ_NOTIFY = 4402;

    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_TITLE = "title";

    /** 由 date+time+title 派生稳定且尽量唯一的 requestCode（避开站立提醒的 4xxx 段） */
    public static int idOf(String date, String time, String title) {
        String s = (date == null ? "" : date) + "|" + (time == null ? "" : time) + "|" + (title == null ? "" : title);
        int h = 1831;
        for (int i = 0; i < s.length(); i++) {
            h = (h * 31 + s.charAt(i)) & 0x7fffffff;
        }
        return (h % 90000) + 10000;
    }

    public static void schedule(Context context, String date, String time, String title, long ts) {
        try {
            if (ts <= System.currentTimeMillis()) return;   // 已过期，不排
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent intent = new Intent(context, TodoAlarmReceiver.class).setAction(ACTION_FIRE);
            intent.putExtra(EXTRA_DATE, date == null ? "" : date);
            intent.putExtra(EXTRA_TIME, time == null ? "" : time);
            intent.putExtra(EXTRA_TITLE, title == null ? "" : title);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(context, idOf(date, time, title), intent, flags);

            boolean exact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                exact = false;
            }
            try {
                if (exact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
                }
            } catch (SecurityException se) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
            }
        } catch (Exception ignored) {
        }
    }

    public static void cancel(Context context, String date, String time, String title) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent intent = new Intent(context, TodoAlarmReceiver.class).setAction(ACTION_FIRE);
            int flags = PendingIntent.FLAG_NO_CREATE;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(context, idOf(date, time, title), intent, flags);
            if (pi != null) am.cancel(pi);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (!ACTION_FIRE.equals(intent == null ? null : intent.getAction())) return;
            String title = intent.getStringExtra(EXTRA_TITLE);
            String time = intent.getStringExtra(EXTRA_TIME);
            String date = intent.getStringExtra(EXTRA_DATE);
            String body = (time != null && !time.isEmpty() ? time + " · " : "")
                    + (title != null && !title.isEmpty() ? title : "待办提醒");
            notify(context, "⏰ 待办提醒", body);
            vibrate(context);
        } catch (Exception ignored) {
        }
    }

    private static void notify(Context context, String title, String body) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            }
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "待办提醒", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("手动待办到点提醒");
                ch.enableVibration(true);
                ch.setVibrationPattern(new long[]{0, 350, 160, 350});
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int cflags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) cflags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent content = PendingIntent.getActivity(context, REQ_NOTIFY, open, cflags);

            NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 350, 160, 350})
                    .setContentIntent(content);
            nm.notify(REQ_NOTIFY, b.build());
        } catch (Exception ignored) {
        }
    }

    private static void vibrate(Context context) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(new long[]{0, 350, 160, 350}, -1));
            } else {
                v.vibrate(new long[]{0, 350, 160, 350}, -1);
            }
        } catch (Exception ignored) {
        }
    }
}
