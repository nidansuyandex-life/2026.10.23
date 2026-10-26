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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_WIDGET_CHECKIN = "com.daily.healthlife.WIDGET_CHECKIN";
    public static final String ACTION_WIDGET_TOGGLE_ITEM = "com.daily.healthlife.WIDGET_TOGGLE_ITEM";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            try { updateWidget(context, appWidgetManager, appWidgetId); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            super.onReceive(context, intent);
            if (ACTION_WIDGET_CHECKIN.equals(intent.getAction())) {
                SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String lastCheckin = sp.getString("last_checkin_date", "");
                if (!today.equals(lastCheckin)) {
                    sp.edit().putBoolean("is_checked_in", true).putString("last_checkin_date", today).apply();
                } else {
                    boolean isCheckedIn = sp.getBoolean("is_checked_in", false);
                    sp.edit().putBoolean("is_checked_in", !isCheckedIn).apply();
                }
                refreshAll(context);
            } else if (ACTION_WIDGET_TOGGLE_ITEM.equals(intent.getAction())) {
                String key = intent.getStringExtra("item_key");
                boolean done = intent.getBooleanExtra("item_done", false);
                
                SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
                String timelineJson = sp.getString("timeline_data", "[]");
                try {
                    JSONArray arr = new JSONArray(timelineJson);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        if (key != null && key.equals(item.optString("key"))) {
                            item.put("done", done);
                            break;
                        }
                    }
                    sp.edit().putString("timeline_data", arr.toString()).apply();
                } catch (Exception e) { e.printStackTrace(); }
                
                refreshAll(context);
                
                Intent broadcast = new Intent(ACTION_WIDGET_TOGGLE_ITEM);
                broadcast.putExtra("item_key", key);
                broadcast.putExtra("item_done", done);
                context.sendBroadcast(broadcast);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void refreshAll(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, HealthWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(componentName);
            for (int id : ids) { updateWidget(context, manager, id); }
        } catch (Exception e) { e.printStackTrace(); }
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

        Intent checkinIntent = new Intent(context, HealthWidgetProvider.class);
        checkinIntent.setAction(ACTION_WIDGET_CHECKIN);
        PendingIntent checkinPi = PendingIntent.getBroadcast(context, 1, checkinIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_checkin, checkinPi);

        Intent moneyIntent = new Intent(context, MainActivity.class);
        moneyIntent.setAction(Intent.ACTION_VIEW);
        moneyIntent.setData(Uri.parse("healthlife://jump?route=money"));
        PendingIntent moneyPi = PendingIntent.getActivity(context, 2, moneyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_expense, moneyPi);

        Intent studyIntent = new Intent(context, MainActivity.class);
        studyIntent.setAction(Intent.ACTION_VIEW);
        studyIntent.setData(Uri.parse("healthlife://jump?route=study"));
        PendingIntent studyPi = PendingIntent.getActivity(context, 3, studyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_progress, studyPi);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setAction(Intent.ACTION_MAIN);
        openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPi = PendingIntent.getActivity(context, 4, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_open_app, openAppPi);

        String timelineJson = sp.getString("timeline_data", "[]");
        try {
            JSONArray arr = new JSONArray(timelineJson);
            List<JSONObject> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getJSONObject(i));
            }
            // 核心逻辑：未完成的排前面，已完成的沉底，同样状态按时间排序
            Collections.sort(list, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    boolean d1 = o1.optBoolean("done");
                    boolean d2 = o2.optBoolean("done");
                    if (d1 == d2) {
                        return o1.optString("time").compareTo(o2.optString("time"));
                    }
                    return d1 ? 1 : -1;
                }
            });

            int[] layouts = {R.id.widget_item_1_layout, R.id.widget_item_2_layout, R.id.widget_item_3_layout, R.id.widget_item_4_layout, R.id.widget_item_5_layout, R.id.widget_item_6_layout, R.id.widget_item_7_layout, R.id.widget_item_8_layout};
            int[] checkIds = {R.id.widget_item_1_check, R.id.widget_item_2_check, R.id.widget_item_3_check, R.id.widget_item_4_check, R.id.widget_item_5_check, R.id.widget_item_6_check, R.id.widget_item_7_check, R.id.widget_item_8_check};
            int[] textIds = {R.id.widget_item_1_text, R.id.widget_item_2_text, R.id.widget_item_3_text, R.id.widget_item_4_text, R.id.widget_item_5_text, R.id.widget_item_6_text, R.id.widget_item_7_text, R.id.widget_item_8_text};

            for (int i = 0; i < 8; i++) {
                if (i < list.size()) {
                    JSONObject item = list.get(i);
                    String time = item.optString("time", "");
                    String text = item.optString("text", "");
                    String key = item.optString("key", "");
                    boolean done = item.optBoolean("done", false);

                    views.setViewVisibility(layouts[i], View.VISIBLE);
                    views.setTextViewText(textIds[i], time + " " + text);
                    
                    if (done) {
                        views.setImageViewResource(checkIds[i], android.R.drawable.checkbox_on_background);
                        views.setTextColor(textIds[i], 0xFF9CA3AF); 
                    } else {
                        views.setImageViewResource(checkIds[i], android.R.drawable.checkbox_off_background);
                        views.setTextColor(textIds[i], 0xFF1B1F2A);
                    }

                    Intent toggleIntent = new Intent(context, HealthWidgetProvider.class);
                    toggleIntent.setAction(ACTION_WIDGET_TOGGLE_ITEM);
                    toggleIntent.putExtra("item_key", key);
                    toggleIntent.putExtra("item_done", !done);
                    PendingIntent pi = PendingIntent.getBroadcast(context, 10 + i, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(layouts[i], pi);
                } else {
                    views.setViewVisibility(layouts[i], View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
