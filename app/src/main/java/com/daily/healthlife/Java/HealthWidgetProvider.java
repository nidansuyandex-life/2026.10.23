package com.daily.healthlife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HealthWidgetProvider extends AppWidgetProvider {
    // 自定义 Action 用于卡片内的打卡广播
    public static final String ACTION_WIDGET_CHECKIN = "com.daily.healthlife.WIDGET_CHECKIN";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // 处理卡片上的打卡点击
        if (ACTION_WIDGET_CHECKIN.equals(intent.getAction())) {
            SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String lastCheckin = sp.getString("last_checkin_date", "");

            if (!today.equals(lastCheckin)) {
                // 新的一天，进行打卡
                sp.edit().putBoolean("is_checked_in", true)
                  .putString("last_checkin_date", today)
                  .apply();
            } else {
                // 如果今天已经打卡，再次点击取消打卡
                boolean isCheckedIn = sp.getBoolean("is_checked_in", false);
                sp.edit().putBoolean("is_checked_in", !isCheckedIn).apply();
            }

            // 刷新所有桌面卡片
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, HealthWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(componentName);
            for (int id : ids) {
                updateWidget(context, manager, id);
            }
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

        SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
        String studyText = sp.getString("study_progress", "今日学习：0/10");
        String expenseText = sp.getString("month_expense", "本月支出：¥0.00");

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastCheckin = sp.getString("last_checkin_date", "");
        boolean isTodayChecked = today.equals(lastCheckin) && sp.getBoolean("is_checked_in", false);

        views.setTextViewText(R.id.widget_progress, studyText);
        views.setTextViewText(R.id.widget_expense, expenseText);
        views.setTextViewText(R.id.widget_btn_checkin, isTodayChecked ? "已打卡" : "打卡");

        // 1. 点击【打卡】按钮
        Intent checkinIntent = new Intent(context, HealthWidgetProvider.class);
        checkinIntent.setAction(ACTION_WIDGET_CHECKIN);
        PendingIntent checkinPi = PendingIntent.getBroadcast(context, 1, checkinIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_checkin, checkinPi);

        // 2. 点击【本月支出】跳转记账页
        Intent moneyIntent = new Intent(context, MainActivity.class);
        moneyIntent.setAction(Intent.ACTION_VIEW);
        moneyIntent.setData(Uri.parse("healthlife://jump?route=money"));
        PendingIntent moneyPi = PendingIntent.getActivity(context, 2, moneyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_expense, moneyPi);

        // 3. 点击【今日学习】跳转首页/学习页
        Intent studyIntent = new Intent(context, MainActivity.class);
        studyIntent.setAction(Intent.ACTION_VIEW);
        studyIntent.setData(Uri.parse("healthlife://jump?route=study"));
        PendingIntent studyPi = PendingIntent.getActivity(context, 3, studyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_progress, studyPi);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
