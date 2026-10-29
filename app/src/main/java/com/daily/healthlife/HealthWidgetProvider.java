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

import org.json.JSONArray;
import org.json.JSONObject;

public class HealthWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_CHECK = "com.daily.healthlife.WIDGET_CHECK";
    public static final String PREFS_NAME = "WidgetPrefs";
    public static final String KEY_TIMELINE_DATA = "widget_timeline_data";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_CHECK.equals(intent.getAction())) {
            String checkKey = intent.getStringExtra("key");
            boolean newDone = intent.getBooleanExtra("done", false);

            // 更新本地记录，避免每次都要等 APP 同步
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(checkKey, newDone).apply();
            
            // 发送本地广播，通知 MainActivity 中的 WebView 同步状态
            Intent syncIntent = new Intent("com.daily.healthlife.WIDGET_CHECK_SYNC");
            syncIntent.putExtra("key", checkKey);
            syncIntent.putExtra("done", newDone);
            context.sendBroadcast(syncIntent);

            // 刷新界面
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, HealthWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

        // 点击右上角打开 APP
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_open_app, openAppPendingIntent);

        // 从 SharedPreferences 获取 JS 推送过来的数据
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_TIMELINE_DATA, "[]");

        try {
            JSONArray arr = new JSONArray(jsonStr);
            int[] layoutIds = {R.id.widget_item_1_layout, R.id.widget_item_2_layout, R.id.widget_item_3_layout, R.id.widget_item_4_layout, R.id.widget_item_5_layout};
            int[] checkIds = {R.id.widget_item_1_check, R.id.widget_item_2_check, R.id.widget_item_3_check, R.id.widget_item_4_check, R.id.widget_item_5_check};
            int[] textIds = {R.id.widget_item_1_text, R.id.widget_item_2_text, R.id.widget_item_3_text, R.id.widget_item_4_text, R.id.widget_item_5_text};
            int[] iconIds = {R.id.widget_item_1_icon, R.id.widget_item_2_icon, R.id.widget_item_3_icon, R.id.widget_item_4_icon, R.id.widget_item_5_icon};

            for (int i = 0; i < 5; i++) {
                if (i < arr.length()) {
                    JSONObject item = arr.getJSONObject(i);
                    String text = item.optString("text", "");
                    String icon = item.optString("icon", "");
                    String key = item.optString("key", "");
                    String route = item.optString("route", "home");
                    boolean done = item.optBoolean("done", false);

                    // 检查本地是否刚点击过（覆盖 JS 传来的状态）
                    if (prefs.contains(key)) {
                        done = prefs.getBoolean(key, done);
                    }

                    views.setViewVisibility(layoutIds[i], android.view.View.VISIBLE);
                    views.setTextViewText(textIds[i], text);
                    views.setTextViewText(iconIds[i], icon);
                    
                    // 圆圈状态
                    if (done) {
                        views.setTextViewText(checkIds[i], "✓");
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg_done);
                    } else {
                        views.setTextViewText(checkIds[i], String.valueOf(i + 1));
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg);
                    }

                    // 1. 点击打卡圆圈
                    Intent checkIntent = new Intent(context, HealthWidgetProvider.class);
                    checkIntent.setAction(ACTION_CHECK);
                    checkIntent.putExtra("key", key);
                    checkIntent.putExtra("done", !done);
                    // requestCode 使用 i 保证每个部件独立
                    PendingIntent checkPendingIntent = PendingIntent.getBroadcast(context, i, checkIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(checkIds[i], checkPendingIntent);

                    // 2. 点击文字或图标，跳转 APP 对应页面
                    Intent routeIntent = new Intent(context, MainActivity.class);
                    // 这里假设你的 MainActivity 会去读取 route 参数并传给 WebView
                    routeIntent.putExtra("route", route);
                    routeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    // requestCode 使用 10+i 避免冲突
                    PendingIntent routePendingIntent = PendingIntent.getActivity(context, 10 + i, routeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(textIds[i], routePendingIntent);
                    views.setOnClickPendingIntent(iconIds[i], routePendingIntent);
                    
                } else {
                    views.setViewVisibility(layoutIds[i], android.view.View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
