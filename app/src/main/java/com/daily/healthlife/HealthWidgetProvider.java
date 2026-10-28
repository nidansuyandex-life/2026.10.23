package com.daily.healthlife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class HealthWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

            // 1. 设置点击打开 APP 的按钮
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_btn_open_app, pendingIntent);

            // 2. 设置默认的静态文案和图标
            int[] textIds = {R.id.widget_item_1_text, R.id.widget_item_2_text, R.id.widget_item_3_text, R.id.widget_item_4_text, R.id.widget_item_5_text};
            int[] iconIds = {R.id.widget_item_1_icon, R.id.widget_item_2_icon, R.id.widget_item_3_icon, R.id.widget_item_4_icon, R.id.widget_item_5_icon};
            int[] checkIds = {R.id.widget_item_1_check, R.id.widget_item_2_check, R.id.widget_item_3_check, R.id.widget_item_4_check, R.id.widget_item_5_check};

            String[] defaultTexts = {"起床", "护肤", "早饭", "上班", "学习"};
            String[] defaultIcons = {"🌅", "🐱", "📝", "📚", "🧴"};

            for (int i = 0; i < 5; i++) {
                views.setTextViewText(textIds[i], defaultTexts[i]);
                views.setTextViewText(iconIds[i], defaultIcons[i]);
                // 默认全是未完成状态
                views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    // 这个方法是供你的 JsBridge 或者外部代码调用的，用来根据 JS 传来的数据更新小部件
    public static void updateWidgetData(Context context, String timelineJson) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new android.content.ComponentName(context, HealthWidgetProvider.class));

        if (appWidgetIds == null || appWidgetIds.length == 0) return;

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

            // 点击打开 APP 的按钮
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_btn_open_app, pendingIntent);

            int[] textIds = {R.id.widget_item_1_text, R.id.widget_item_2_text, R.id.widget_item_3_text, R.id.widget_item_4_text, R.id.widget_item_5_text};
            int[] iconIds = {R.id.widget_item_1_icon, R.id.widget_item_2_icon, R.id.widget_item_3_icon, R.id.widget_item_4_icon, R.id.widget_item_5_icon};
            int[] checkIds = {R.id.widget_item_1_check, R.id.widget_item_2_check, R.id.widget_item_3_check, R.id.widget_item_4_check, R.id.widget_item_5_check};

            try {
                JSONArray array = new JSONArray(timelineJson);
                int count = Math.min(array.length(), 5);

                for (int i = 0; i < 5; i++) {
                    if (i < count) {
                        JSONObject item = array.getJSONObject(i);
                        String text = item.optString("text", "");
                        String icon = item.optString("icon", "📝");
                        boolean isDone = item.optBoolean("done", false);

                        // 更新文字和图标
                        views.setTextViewText(textIds[i], text);
                        views.setTextViewText(iconIds[i], icon);

                        // 核心修复点：根据状态切换背景，使用你项目中实际存在的文件
                        if (isDone) {
                            views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg_done);
                        } else {
                            views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg);
                        }
                    } else {
                        // 如果数据不足 5 条，隐藏多余的
                        views.setTextViewText(textIds[i], "");
                        views.setTextViewText(iconIds[i], "");
                        views.setInt(checkIds[i], "setBackgroundResource", R.drawable.widget_checkbox_bg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
