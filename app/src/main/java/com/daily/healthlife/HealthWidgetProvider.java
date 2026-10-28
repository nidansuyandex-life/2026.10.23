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
    public static final String ACTION_WIDGET_TOGGLE_ITEM = "com.daily.healthlife.WIDGET_TOGGLE_ITEM";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            try { updateWidget(context, appWidgetManager, appWidgetId); } catch (Throwable t) { t.printStackTrace(); }
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
                } catch (Exception ignored) {}
                
                refreshAll(context);
            }
        } catch (Throwable t) { t.printStackTrace(); }
    }

    private void refreshAll(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, HealthWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(componentName);
            for (int id : ids) { updateWidget(context, manager, id); }
        } catch (Throwable t) { t.printStackTrace(); }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

        // 1. 打开APP按钮
        try {
            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.setAction(Intent.ACTION_MAIN);
            openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openAppPi = PendingIntent.getActivity(context, 4, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_btn_open_app, openAppPi);
        } catch (Exception ignored) {}

        // 2. 渲染列表
        try {
            SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
            String timelineJson = sp.getString("timeline_data", "[]");
            String lastSyncDate = sp.getString("last_sync_date", "");
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
            
            JSONArray arr = new JSONArray(timelineJson);
            List<JSONObject> list = new ArrayList<>();
            
            // 3. ★ 核心逻辑：每天自动刷新（如果是跨天，把所有 done 状态清空）
            boolean isCrossDay = !today.equals(lastSyncDate);
            
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (isCrossDay) {
                    item.put("done", false); // 新的一天，全部重置为未完成
                }
                list.add(item);
            }
            
            if (isCrossDay && list.size() > 0) {
                sp.edit().putString("timeline_data", new JSONArray(list).toString()).putString("last_sync_date", today).apply();
            }

            if (list.isEmpty()) {
                for (int i = 0; i < 5; i++) {
                    JSONObject dummy = new JSONObject();
                    dummy.put("text", "请打开App同步数据");
                    dummy.put("icon", "⚠️");
                    dummy.put("key", "dummy_" + i);
                    dummy.put("done", false);
                    list.add(dummy);
                }
            }
            
            // 排序：先按时间
            Collections.sort(list, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    return o1.optString("time").compareTo(o2.optString("time"));
                }
            });

            // 排序：未完成移上来，已完成下沉
            List<JSONObject> sortedList = new ArrayList<>();
            for (JSONObject item : list) if (!item.optBoolean("done", false)) sortedList.add(item);
            for (JSONObject item : list) if (item.optBoolean("done", false)) sortedList.add(item);

            int[] layouts = {R.id.widget_item_1_layout, R.id.widget_item_2_layout, R.id.widget_item_3_layout, R.id.widget_item_4_layout, R.id.widget_item_5_layout};
            int[] icons = {R.id.widget_item_1_icon, R.id.widget_item_2_icon, R.id.widget_item_3_icon, R.id.widget_item_4_icon, R.id.widget_item_5_icon};
            int[] textIds = {R.id.widget_item_1_text, R.id.widget_item_2_text, R.id.widget_item_3_text, R.id.widget_item_4_text, R.id.widget_item_5_text};
            int[] checkIds = {R.id.widget_item_1_check, R.id.widget_item_2_check, R.id.widget_item_3_check, R.id.widget_item_4_check, R.id.widget_item_5_check};

            for (int i = 0; i < 5; i++) {
                if (i < sortedList.size()) {
                    JSONObject item = sortedList.get(i);
                    String text = item.optString("text", "");
                    String icon = item.optString("icon", "✅");
                    String key = item.optString("key", "");
                    boolean done = item.optBoolean("done", false);

                    views.setViewVisibility(layouts[i], View.VISIBLE);
                    views.setTextViewText(textIds[i], text);
                    views.setTextViewText(icons[i], icon);
                    
                    if (done) {
                        views.setTextViewText(checkIds[i], "✓");
                        views.setTextColor(checkIds[i], 0xFFFFFFFF);
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_square_bg_done);
                        views.setTextColor(textIds[i], 0xFF9CA3AF);
                    } else {
                        views.setTextViewText(checkIds[i], String.valueOf(i + 1));
                        views.setTextColor(checkIds[i], 0xFF9CA3AF);
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_square_bg);
                        views.setTextColor(textIds[i], 0xFF111827);
                    }

                    if (!key.startsWith("dummy_")) {
                        // A. 点击左侧勾选框 -> 切换状态
                        Intent toggleIntent = new Intent(context, HealthWidgetProvider.class);
                        toggleIntent.setAction(ACTION_WIDGET_TOGGLE_ITEM);
                        toggleIntent.putExtra("item_key", key);
                        toggleIntent.putExtra("item_done", !done);
                        PendingIntent togglePi = PendingIntent.getBroadcast(context, 10 + i, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setOnClickPendingIntent(checkIds[i], togglePi);

                        // B. 点击整行 -> 跳转 APP 指定页面
                        String route = getRouteFromKey(key);
                        Intent jumpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("healthlife://jump?route=" + route));
                        jumpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        PendingIntent jumpPi = PendingIntent.getActivity(context, 200 + i, jumpIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setOnClickPendingIntent(layouts[i], jumpPi);
                    }
                } else {
                    views.setViewVisibility(layouts[i], View.GONE);
                }
            }
        } catch (Throwable t) { 
            t.printStackTrace(); 
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // 根据 Key 决定跳转到哪个页面
    private String getRouteFromKey(String key) {
        if (key == null) return "home";
        if (key.contains("study") || key.contains("commute")) return "study";
        if (key.contains("skin")) return "care_skin";
        if (key.contains("breakfast") || key.contains("lunch") || key.contains("dinner") || key.contains("protein")) return "care_supp";
        if (key.contains("sport")) return "sport";
        if (key.contains("todo")) return "calendar";
        return "home"; // 默认回首页
    }
}
