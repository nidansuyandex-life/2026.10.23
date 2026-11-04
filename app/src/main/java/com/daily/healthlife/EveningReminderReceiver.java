package com.daily.healthlife;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Locale;

/**
 * 晚间小结提醒的原生调度器。
 *
 * 每天在设定时刻（默认 20:30）温和地提醒一次：
 *  - App 在前台：调页面里的 onNativeEveningReminder()，弹个轻提示
 *  - App 在后台/被清理：发一条普通优先级通知，文案优先用页面
 *    同步过来的「今日未完成摘要」（saveEveningBrief），没有就用默认文案
 *  - 点通知直达时间线（healthlife://jump?route=calendar）
 *
 * App 关掉、手机重启后依然准点触发（AlarmManager + BOOT_COMPLETED 重排）。
 */
public class EveningReminderReceiver extends BroadcastReceiver {

    public static final String PREFS = "EveningSettings";
    public static final String ACTION_FIRE = "com.daily.healthlife.EVENING_ALARM";
    private static final int REQ_ALARM = 4401;
    private static final int REQ_NOTIFY = 4402;
    private static final String CHANNEL = "evening_reminder";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        String action = intent == null ? null : intent.getAction();

        // 开机 / 应用更新后重排闹钟
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            scheduleNext(context);
            return;
        }

        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!sp.getBoolean("enabled", false)) return;

        // 时间复核：闹钟被系统严重延迟（超过设定时刻 45 分钟以上）就跳过，明天再来
        int set = sp.getInt("minute", 20 * 60 + 30);
        Calendar c = Calendar.getInstance();
        int nowMin = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        if (nowMin < set - 5 || nowMin > set + 45) {
            scheduleNext(context);
            return;
        }

        // 同一天只提醒一次
        String today = todayKey();
        if (today.equals(sp.getString("lastDay", ""))) {
            scheduleNext(context);
            return;
        }
        sp.edit().putString("lastDay", today).apply();

        boolean handledByPage = false;
        if (MainActivity.isAppForeground()) {
            handledByPage = MainActivity.runJs("window.onNativeEveningReminder && window.onNativeEveningReminder()");
        }
        if (!handledByPage) {
            showNotification(context, sp);
        }

        scheduleNext(context);
    }

    /** 排下一次提醒（今天的时刻已过就排明天） */
    public static void scheduleNext(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = pending(ctx);
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            if (!sp.getBoolean("enabled", false)) {
                am.cancel(pi);
                return;
            }

            int minute = Math.max(0, Math.min(23 * 60 + 59, sp.getInt("minute", 20 * 60 + 30)));
            long at = nextTrigger(minute, System.currentTimeMillis());

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
        Intent i = new Intent(ctx, EveningReminderReceiver.class).setAction(ACTION_FIRE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQ_ALARM, i, flags);
    }

    /** 从当前时刻往后找下一个提醒时间点 */
    static long nextTrigger(int minute, long nowMs) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMs);
        for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
            Calendar t = (Calendar) now.clone();
            t.add(Calendar.DAY_OF_YEAR, dayOffset);
            t.set(Calendar.HOUR_OF_DAY, minute / 60);
            t.set(Calendar.MINUTE, minute % 60);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            if (t.getTimeInMillis() > nowMs + 20000L) return t.getTimeInMillis();
        }
        return nowMs + 24L * 3600L * 1000L;
    }

    private static String todayKey() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private void showNotification(Context ctx, SharedPreferences sp) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "晚间小结提醒", NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("睡前提醒收今天的星光");
                nm.createNotificationChannel(ch);
            }

            // 优先用页面同步来的「今日摘要」，过期或没有就用默认文案
            String text = "今天还有没完成的事吗？来收今天的星光 ✦";
            try {
                if (todayKey().equals(sp.getString("briefDate", ""))) {
                    String brief = sp.getString("briefText", "");
                    if (brief != null && !brief.trim().isEmpty()) text = brief;
                }
            } catch (Exception ignored) {
            }

            Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("healthlife://jump?route=calendar"));
            open.setPackage(ctx.getPackageName());
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent content = PendingIntent.getActivity(ctx, REQ_NOTIFY, open, flags);

            NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("晚间小结")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(content);
            nm.notify(REQ_NOTIFY, b.build());
        } catch (Exception ignored) {
        }
    }
}
