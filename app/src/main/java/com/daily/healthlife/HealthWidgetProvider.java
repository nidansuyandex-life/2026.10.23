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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_WIDGET_TOGGLE_ITEM = "com.daily.healthlife.WIDGET_TOGGLE_ITEM";

    @Override 
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) { 
            try { updateWidget(context, appWidgetManager, id); } catch (Throwable t) { t.printStackTrace(); } 
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
                
                try {
                    JSONArray arr = new JSONArray(sp.getString("timeline_data", "[]"));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        if (key != null && key.equals(item.optString("key"))) { 
                            item.put("done", done); 
                            break; 
                        }
                    }
                    sp.edit().putString("timeline_data", arr.toString()).apply();
                } catch (Exception ignored) {}
                
                refreshAll(context);
            }
        } catch (Throwable t) { t.printStackTrace(); }
    }

    private void refreshAll(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            for (int id : manager.getAppWidgetIds(new ComponentName(context, HealthWidgetProvider.class))) {
                updateWidget(context, manager, id);
            }
        } catch (Throwable t) { t.printStackTrace(); }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);
        
        // 点击顶部“打开APP”
        try {
            Intent openAppIntent = new Intent(context, MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            views.setOnClickPendingIntent(R.id.widget_btn_open_app, PendingIntent.getActivity(context, 4, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } catch (Exception ignored) {}

        try {
            SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
            String timelineJson = sp.getString("timeline_data", "[]");
            String lastSyncDate = sp.getString("last_sync_date", "");
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
            
            JSONArray arr = new JSONArray(timelineJson);
            List<JSONObject> list = new ArrayList<>();
            boolean isCrossDay = !today.equals(lastSyncDate);
            
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (isCrossDay) item.put("done", false); // 跨天自动清空打卡
                list.add(item);
            }
            if (isCrossDay && !list.isEmpty()) {
                sp.edit().putString("timeline_data", new JSONArray(list).toString()).putString("last_sync_date", today).apply();
            }

            // 空数据占位
            if (list.isEmpty()) {
                for (int i = 0; i < 5; i++) {
                    JSONObject dummy = new JSONObject();
                    dummy.put("text", "请打开App同步数据"); 
                    dummy.put("icon", "⚠️"); 
                    dummy.put("key", "dummy_" + i); 
                    dummy.put("done", false); 
                    dummy.put("time", "99:99");
                    list.add(dummy);
                }
            }

            // 与 APP 时间线保持一致：只按时间排序，不把已完成事项重新挪位。
            Collections.sort(list, (o1, o2) ->
                    o1.optString("time", "99:99").compareTo(o2.optString("time", "99:99")));

            int[] layouts = {R.id.widget_item_1_layout, R.id.widget_item_2_layout, R.id.widget_item_3_layout, R.id.widget_item_4_layout, R.id.widget_item_5_layout};
            int[] icons = {R.id.widget_item_1_icon, R.id.widget_item_2_icon, R.id.widget_item_3_icon, R.id.widget_item_4_icon, R.id.widget_item_5_icon};
            int[] textIds = {R.id.widget_item_1_text, R.id.widget_item_2_text, R.id.widget_item_3_text, R.id.widget_item_4_text, R.id.widget_item_5_text};
            int[] checkIds = {R.id.widget_item_1_check, R.id.widget_item_2_check, R.id.widget_item_3_check, R.id.widget_item_4_check, R.id.widget_item_5_check};

            for (int i = 0; i < 5; i++) {
                if (i < list.size()) {
                    JSONObject item = list.get(i);
                    String text = item.optString("text", ""); 
                    String icon = item.optString("icon", "✅");
                    String key = item.optString("key", ""); 
                    String time = item.optString("time", "");
                    boolean done = item.optBoolean("done", false);

                    views.setViewVisibility(layouts[i], View.VISIBLE);
                    views.setTextViewText(icons[i], icon); 
                    views.setTextViewText(textIds[i], time.isEmpty() ? text : time + " " + text);
                    
                    if (done) {
                        views.setTextViewText(checkIds[i], "✓"); 
                        views.setTextColor(checkIds[i], 0xFFFFFFFF);
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_square_bg_done);
                        views.setTextColor(textIds[i], 0xFF9CA3AF);
                    } else {
                        views.setTextViewText(checkIds[i], ""); 
                        views.setTextColor(checkIds[i], 0xFF9CA3AF);
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_square_bg);
                        views.setTextColor(textIds[i], 0xFF111827);
                    }

                    if (!key.startsWith("dummy_")) {
                        // 点击打卡框 -> 切换状态
                        Intent toggleIntent = new Intent(context, HealthWidgetProvider.class).setAction(ACTION_WIDGET_TOGGLE_ITEM).putExtra("item_key", key).putExtra("item_done", !done);
                        views.setOnClickPendingIntent(checkIds[i], PendingIntent.getBroadcast(context, 10 + i, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

                        // 点击文字 -> 跳转App
                        String route = getRouteFromKey(key);
                        Intent jumpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("healthlife://jump?route=" + route)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        views.setOnClickPendingIntent(layouts[i], PendingIntent.getActivity(context, 200 + i, jumpIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                    } else {
                        views.setOnClickPendingIntent(checkIds[i], null);
                        views.setOnClickPendingIntent(layouts[i], null);
                    }
                } else {
                    views.setViewVisibility(layouts[i], View.GONE);
                }
            }
        } catch (Throwable t) { t.printStackTrace(); }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private String getRouteFromKey(String key) {
        if (key == null) return "home";
        if (key.contains("study") || key.contains("commute")) return "study";
        if (key.contains("skin")) return "care_skin";
        if (key.contains("breakfast") || key.contains("lunch") || key.contains("dinner") || key.contains("protein")) return "care_supp";
        if (key.contains("sport")) return "sport";
        if (key.contains("todo")) return "calendar";
        return "home";
    }
}
