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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HealthWidgetProvider extends AppWidgetProvider {
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
            if (ACTION_WIDGET_TOGGLE_ITEM.equals(intent.getAction())) {
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
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, HealthWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        for (int id : ids) { updateWidget(context, manager, id); }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

        SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
        String expenseText = sp.getString("month_expense", "0");
        // 只取数字部分，比如把 "本月支出：¥15" 变成 "15"
        String expenseNum = expenseText.replaceAll("[^0-9.]", "");
        if (expenseNum.isEmpty()) expenseNum = "0";

        views.setTextViewText(R.id.widget_expense, expenseNum);

        // 打开APP
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setAction(Intent.ACTION_MAIN);
        openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPi = PendingIntent.getActivity(context, 4, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_open_app, openAppPi);

        // 读取时间线
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

            int[] layouts = {R.id.widget_item_1_layout, R.id.widget_item_2_layout, R.id.widget_item_3_layout, R.id.widget_item_4_layout, R.id.widget_item_5_layout};
            int[] icons = {R.id.widget_item_1_icon, R.id.widget_item_2_icon, R.id.widget_item_3_icon, R.id.widget_item_4_icon, R.id.widget_item_5_icon};
            int[] textIds = {R.id.widget_item_1_text, R.id.widget_item_2_text, R.id.widget_item_3_text, R.id.widget_item_4_text, R.id.widget_item_5_text};
            int[] checkIds = {R.id.widget_item_1_check, R.id.widget_item_2_check, R.id.widget_item_3_check, R.id.widget_item_4_check, R.id.widget_item_5_check};

            for (int i = 0; i < 5; i++) {
                if (i < list.size()) {
                    JSONObject item = list.get(i);
                    String time = item.optString("time", "");
                    String text = item.optString("text", "");
                    String icon = item.optString("icon", "✅");
                    String key = item.optString("key", "");
                    boolean done = item.optBoolean("done", false);

                    views.setViewVisibility(layouts[i], View.VISIBLE);
                    
                    // 如果只有时间没有文字，比如 "07:40 "，就显示时间。如果有文字就 "时间 文字"
                    views.setTextViewText(icons[i], icon);
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
