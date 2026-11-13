package com.daily.healthlife;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

/**
 * 站立走动提醒的原生调度器。
 *
 * 之前「到点该提醒了」这件事完全靠 WebView 里的 setInterval 判断，
 * App 被系统回收或长时间后台冻结后就不会响。这里用 AlarmManager
 * 在系统层面排下一次提醒，App 关掉也能准点触发。
 *
 * 规则（与页面设置一致）：
 *  - 只在周一至周五
 *  - 在 [startMin, endMin] 区间内，按 freq 分钟步进
 *  - 11:30–13:00 午休静音
 */
public class WalkAlarmReceiver extends BroadcastReceiver {

    public static final String PREFS = "WalkSettings";
    public static final String ACTION_FIRE = "com.daily.healthlife.WALK_ALARM";
    private static final int REQ_ALARM = 4301;
    private static final int REQ_NOTIFY = 4302;
    private static final String CHANNEL = "walk_reminder";
    private static final int LUNCH_START = 11 * 60 + 30;   // 690
    private static final int LUNCH_END = 13 * 60;         // 780

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String action = intent == null ? null : intent.getAction();

        // 开机 / 应用更新后重排闹钟（系统重启会清掉所有闹钟）
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            scheduleNext(context);
            return;
        }

        if (!sp.getBoolean("enabled", true)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!inWindow(sp, now)) {
            // 闹钟被系统延迟到时段外（比如深夜），跳过并重排
            scheduleNext(context);
            return;
        }

        // 已经提醒过的这一分钟不再重复（避免闹钟抖动）
        int slot = slotOf(now);
        if (sp.getInt("lastSlot", -1) == slot) {
            scheduleNext(context);
            return;
        }
        sp.edit().putInt("lastSlot", slot).apply();

        // 开始一次走动倒计时：原生前台服务负责「通知栏实时倒计时 + 后台到点提醒」；
        // 无论如何都先启动它（前后台都启动，依赖精确闹钟的系统豁免），前台时页面再补一个居中大数字 UI。
        startWalkTimer(context, 200);

        scheduleNext(context);
    }

    /**
     * 启动走动倒计时。
     * 优先原生前台服务（通知栏实时倒计时 + 后台提醒）。若后台启动前台服务被系统拒绝
     * （Android 12+ 限制 / 国产 ROM 激进清理），则降级为静态通知 + 一条兜底「时间到」精确闹钟，
     * 保证至少「到时间」能提醒、且现在就能在通知栏看到提示。
     */
    private void startWalkTimer(Context ctx, int seconds) {
        boolean started = false;
        if (!WalkTimerService.sRunning) {
            try {
                Intent si = new Intent(ctx, WalkTimerService.class);
                si.putExtra("duration", seconds);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(si);
                } else {
                    ctx.startService(si);
                }
                started = true;
            } catch (Throwable t) {
                android.util.Log.w("WalkAlarmReceiver", "startForegroundService denied, fallback", t);
                started = false;
            }
        }
        if (!started) {
            // 后台启动前台服务被拒：降级方案（静态通知 + 兜底闹钟）
            try { WalkEndReceiver.schedule(ctx, seconds); } catch (Throwable ignored) {}
            showNotification(ctx);
        }
        vibrate(ctx);
        // 前台时仍让页面弹出居中倒计时 UI（WebView 内大数字）
        if (MainActivity.isAppForeground()) {
            MainActivity.runJs("window.onNativeWalkAlarm && window.onNativeWalkAlarm()");
        }
    }

    /** 排下一次提醒 */
    public static void scheduleNext(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = pending(ctx);
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            if (!sp.getBoolean("enabled", true)) {
                am.cancel(pi);
                return;
            }

            int start = sp.getInt("startMin", 8 * 60 + 30);
            int end = sp.getInt("endMin", 17 * 60);
            int freq = Math.max(15, sp.getInt("freq", 30));
            long at = nextTrigger(start, end, freq, System.currentTimeMillis());

            boolean exact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                exact = false;
            }
            try {
                if (exact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                }
            } catch (SecurityException se) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (Exception ignored) {
        }
    }

    public static void cancel(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pending(ctx));
        } catch (Exception ignored) {
        }
    }

    private static PendingIntent pending(Context ctx) {
        Intent i = new Intent(ctx, WalkAlarmReceiver.class).setAction(ACTION_FIRE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQ_ALARM, i, flags);
    }

    /** 从当前时刻往后找第一个该提醒的时间点 */
    static long nextTrigger(int startMin, int endMin, int freq, long nowMs) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMs);
        for (int dayOffset = 0; dayOffset < 9; dayOffset++) {
            Calendar day = (Calendar) now.clone();
            day.add(Calendar.DAY_OF_YEAR, dayOffset);
            int dow = day.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) continue;
            for (int m = startMin; m <= endMin; m += freq) {
                if (m > LUNCH_START && m < LUNCH_END) continue;   // 午休静音
                Calendar t = (Calendar) day.clone();
                t.set(Calendar.HOUR_OF_DAY, m / 60);
                t.set(Calendar.MINUTE, m % 60);
                t.set(Calendar.SECOND, 0);
                t.set(Calendar.MILLISECOND, 0);
                if (t.getTimeInMillis() > nowMs + 20000L) return t.getTimeInMillis();
            }
        }
        return nowMs + 24L * 3600L * 1000L;
    }

    /** 当前是否处在提醒时段内（闹钟可能被延迟，触发时要再核一次） */
    private boolean inWindow(SharedPreferences sp, long nowMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(nowMs);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return false;
        int mins = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int start = sp.getInt("startMin", 8 * 60 + 30);
        int end = sp.getInt("endMin", 17 * 60);
        if (mins < start - 3 || mins > end + 3) return false;
        if (mins > LUNCH_START && mins < LUNCH_END) return false;
        return true;
    }

    /** 同一天同一分钟的唯一标识，用于去重 */
    private int slotOf(long nowMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(nowMs);
        return c.get(Calendar.DAY_OF_YEAR) * 10000
                + c.get(Calendar.HOUR_OF_DAY) * 100
                + c.get(Calendar.MINUTE);
    }

    private void showNotification(Context ctx) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "站立走动提醒", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("工作日定时提醒起身活动");
                ch.enableVibration(true);
                ch.setVibrationPattern(new long[]{0, 400, 180, 400});
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(ctx, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent content = PendingIntent.getActivity(ctx, REQ_NOTIFY, open, flags);

            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("该站起来走动了")
                    .setContentText("活动 200 秒，回来继续～")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 400, 180, 400})
                    .setContentIntent(content);
            nm.notify(REQ_NOTIFY, b.build());
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
