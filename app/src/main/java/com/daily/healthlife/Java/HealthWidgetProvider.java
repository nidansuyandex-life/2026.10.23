package com.daily.healthlife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HealthWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_WIDGET_CHECKIN = "com.daily.healthlife.WIDGET_CHECKIN";
    public static final String ACTION_WIDGET_TODO = "com.daily.healthlife.WIDGET_TODO";
    public static final String ACTION_WIDGET_PAGE = "com.daily.healthlife.WIDGET_PAGE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        
        if (ACTION_WIDGET_CHECKIN.equals(action)) {
            SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String lastCheckin = sp.getString("last_checkin_date", "");

            if (!today.equals(lastCheckin)) {
                sp.edit().putBoolean("is_checked_in", true).putString("last_checkin_date", today).apply();
            } else {
                boolean isCheckedIn = sp.getBoolean("is_checked_in", false);
                sp.edit().putBoolean("is_checked_in", !isCheckedIn).apply();
            }
            refreshAllWidgets(context);
        } 
        else if (ACTION_WIDGET_TODO.equals(action)) {
            String itemKey = intent.getStringExtra("item_key");
            if (itemKey != null) {
                Intent jumpIntent = new Intent(context, MainActivity.class);
                jumpIntent.setAction(Intent.ACTION_VIEW);
                jumpIntent.setData(Uri.parse("healthlife://jump?route=toggleTodo&key=" + Uri.encode(itemKey)));
                jumpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(jumpIntent);
            }
        }
        else if (ACTION_WIDGET_PAGE.equals(action)) {
            int delta = intent.getIntExtra("delta", 0);
            SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
            int currentPage = sp.getInt("timeline_page", 0);
            int newPage = currentPage + delta;
            if (newPage >= 0) {
                sp.edit().putInt("timeline_page", newPage).apply();
                refreshAllWidgets(context);
            }
        }
    }
    
    private void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, HealthWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);
        
        try {
            SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);

            String studyText = sp.getString("study_progress", "今日学习：0/10");
            String expenseText = sp.getString("month_expense", "本月支出：¥0.00");
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String lastCheckin = sp.getString("last_checkin_date", "");
            boolean isTodayChecked = today.equals(lastCheckin) && sp.getBoolean("is_checked_in", false);

            views.setTextViewText(R.id.widget_progress, studyText);
            views.setTextViewText(R.id.widget_expense, expenseText);
            views.setTextViewText(R.id.widget_btn_checkin, isTodayChecked ? "已打卡" : "打卡");

            // 绑定点击事件
            Intent checkinIntent = new Intent(context, HealthWidgetProvider.class);
            checkinIntent.setAction(ACTION_WIDGET_CHECKIN);
            views.setOnClickPendingIntent(R.id.widget_btn_checkin, PendingIntent.getBroadcast(context, 1, checkinIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

            Intent moneyIntent = new Intent(context, MainActivity.class);
            moneyIntent.setAction(Intent.ACTION_VIEW);
            moneyIntent.setData(Uri.parse("healthlife://jump?route=money"));
            views.setOnClickPendingIntent(R.id.widget_expense, PendingIntent.getActivity(context, 2, moneyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

            Intent studyIntent = new Intent(context, MainActivity.class);
            studyIntent.setAction(Intent.ACTION_VIEW);
            studyIntent.setData(Uri.parse("healthlife://jump?route=study"));
            views.setOnClickPendingIntent(R.id.widget_progress, PendingIntent.getActivity(context, 3, studyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

            // 动态渲染 15 条待办
            String timelineJson = sp.getString("timeline_data", "[]");
            JSONArray arr = new JSONArray(timelineJson);
            int totalItems = arr.length();
            int pageSize = 15;
            int maxPage = (int) Math.ceil((double) totalItems / pageSize);
            if (maxPage == 0) maxPage = 1;

            int currentPage = sp.getInt("timeline_page", 0);
            if (currentPage >= maxPage) {
                currentPage = 0;
                sp.edit().putInt("timeline_page", 0).apply();
            }

            if (totalItems > pageSize) {
                views.setViewVisibility(R.id.widget_page_control, View.VISIBLE);
                views.setViewVisibility(R.id.widget_page_indicator, View.VISIBLE);
                views.setTextViewText(R.id.widget_page_indicator, (currentPage + 1) + "/" + maxPage);
                views.setViewVisibility(R.id.widget_btn_prev, currentPage > 0 ? View.VISIBLE : View.INVISIBLE);
                views.setViewVisibility(R.id.widget_btn_next, currentPage < maxPage - 1 ? View.VISIBLE : View.INVISIBLE);
                
                Intent prevIntent = new Intent(context, HealthWidgetProvider.class);
                prevIntent.setAction(ACTION_WIDGET_PAGE);
                prevIntent.putExtra("delta", -1);
                views.setOnClickPendingIntent(R.id.widget_btn_prev, PendingIntent.getBroadcast(context, 101, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

                Intent nextIntent = new Intent(context, HealthWidgetProvider.class);
                nextIntent.setAction(ACTION_WIDGET_PAGE);
                nextIntent.putExtra("delta", 1);
                views.setOnClickPendingIntent(R.id.widget_btn_next, PendingIntent.getBroadcast(context, 102, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            } else {
                views.setViewVisibility(R.id.widget_page_control, View.GONE);
                views.setViewVisibility(R.id.widget_page_indicator, View.GONE);
            }

            int startIdx = currentPage * pageSize;

            int[] itemIds = {R.id.widget_todo_item_1, R.id.widget_todo_item_2, R.id.widget_todo_item_3, R.id.widget_todo_item_4, R.id.widget_todo_item_5, R.id.widget_todo_item_6, R.id.widget_todo_item_7, R.id.widget_todo_item_8, R.id.widget_todo_item_9, R.id.widget_todo_item_10, R.id.widget_todo_item_11, R.id.widget_todo_item_12, R.id.widget_todo_item_13, R.id.widget_todo_item_14, R.id.widget_todo_item_15};
            int[] checkIds = {R.id.widget_todo_check_1, R.id.widget_todo_check_2, R.id.widget_todo_check_3, R.id.widget_todo_check_4, R.id.widget_todo_check_5, R.id.widget_todo_check_6, R.id.widget_todo_check_7, R.id.widget_todo_check_8, R.id.widget_todo_check_9, R.id.widget_todo_check_10, R.id.widget_todo_check_11, R.id.widget_todo_check_12, R.id.widget_todo_check_13, R.id.widget_todo_check_14, R.id.widget_todo_check_15};
            int[] textIds = {R.id.widget_todo_text_1, R.id.widget_todo_text_2, R.id.widget_todo_text_3, R.id.widget_todo_text_4, R.id.widget_todo_text_5, R.id.widget_todo_text_6, R.id.widget_todo_text_7, R.id.widget_todo_text_8, R.id.widget_todo_text_9, R.id.widget_todo_text_10, R.id.widget_todo_text_11, R.id.widget_todo_text_12, R.id.widget_todo_text_13, R.id.widget_todo_text_14, R.id.widget_todo_text_15};

            for (int i = 0; i < pageSize; i++) {
                int dataIdx = startIdx + i;
                if (dataIdx < totalItems) {
                    JSONObject obj = arr.getJSONObject(dataIdx);
                    String text = obj.optString("time", "") + " " + obj.optString("title", "");
                    boolean done = obj.optBoolean("done", false);
                    String key = obj.optString("key", "");

                    views.setViewVisibility(itemIds[i], View.VISIBLE);
                    views.setTextViewText(textIds[i], text);

                    // 使用 setBackgroundResource 代替 setBackgroundColor，避免底层崩溃
                    if (done) {
                        views.setTextViewText(checkIds[i], "✓");
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg_done);
                    } else {
                        views.setTextViewText(checkIds[i], "");
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg);
                    }

                    Intent todoIntent = new Intent(context, HealthWidgetProvider.class);
                    todoIntent.setAction(ACTION_WIDGET_TODO);
                    todoIntent.putExtra("item_key", key);
                    views.setOnClickPendingIntent(itemIds[i], PendingIntent.getBroadcast(context, 10 + i, todoIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                } else {
                    views.setViewVisibility(itemIds[i], View.GONE);
                }
            }

        } catch (Exception e) {
            // 捕获所有异常：即便数据烂了，也只显示默认文本，绝不让卡片崩溃变白板
            e.printStackTrace();
            views.setTextViewText(R.id.widget_progress, "今日学习：--");
            views.setTextViewText(R.id.widget_expense, "本月支出：¥--");
            views.setTextViewText(R.id.widget_btn_checkin, "打卡");
            views.setViewVisibility(R.id.widget_page_control, View.GONE);
            for(int i=1; i<=15; i++){
                // 这里不方便循环动态找ID，直接隐藏控制条即可，内部超出部分会自动收起或报错，但外层 try 已经拦住了
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
