package com.daily.healthlife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HealthWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "HealthWidget";
    private static final String ACTION_TOGGLE = "com.daily.healthlife.ACTION_TOGGLE_TASK";
    // 注意：这里请替换为你实际的 SP 名字和 Key
    private static final String SP_NAME = "HealthWidgetData"; 
    private static final String SP_KEY_DATA = "timeline_data";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            try {
                updateWidget(context, appWidgetManager, appWidgetId);
            } catch (Exception e) {
                Log.e(TAG, "onUpdate crash prevented: " + e.getMessage());
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        try {
            if (ACTION_TOGGLE.equals(intent.getAction())) {
                String taskKey = intent.getStringExtra("task_key");
                if (taskKey != null) {
                    toggleTask(context, taskKey);
                    // 刷新所有小部件
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new android.content.ComponentName(context, HealthWidgetProvider.class));
                    for (int appWidgetId : appWidgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onReceive crash prevented: " + e.getMessage());
        }
    }

    private void toggleTask(Context context, String taskKey) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            String jsonStr = sp.getString(SP_KEY_DATA, "[]");
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.optString("key").equals(taskKey)) {
                    boolean done = obj.optBoolean("done", false);
                    obj.put("done", !done);
                    break;
                }
            }
            sp.edit().putString(SP_KEY_DATA, array.toString()).apply();

            // 发送广播通知 MainActivity 让 H5 同步
            Intent intent = new Intent("com.daily.healthlife.ACTION_SYNC_TO_H5");
            intent.setPackage(context.getPackageName()); // 显式指定包名，防止 Android 8.0+ 隐式广播限制
            intent.putExtra("data", array.toString());
            context.sendBroadcast(intent);

        } catch (Exception e) {
            Log.e(TAG, "toggleTask error: " + e.getMessage());
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

        try {
            // 1. 设置左侧数据 (假设你SP里存了金额和日期)
            // views.setTextViewText(R.id.tv_money, "1200"); // 这里读取你的 SP
            // views.setTextViewText(R.id.tv_date, "6月26日");

            // 打开APP按钮
            Intent openIntent = new Intent(context, MainActivity.class);
            PendingIntent openPi = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.tv_open_app, openPi);

            // 2. 解析 JSON 数据
            SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            String jsonStr = sp.getString(SP_KEY_DATA, "[]");
            JSONArray array = new JSONArray(jsonStr);
            
            List<JSONObject> taskList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                taskList.add(array.getJSONObject(i));
            }

            // 3. 智能下沉排序 (未完成在前，完成在后)
            Collections.sort(taskList, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    boolean done1 = o1.optBoolean("done", false);
                    boolean done2 = o2.optBoolean("done", false);
                    return Boolean.compare(done1, done2); // false(0) 排前面，true(1) 排后面
                }
            });

            // 4. 渲染前 5 个任务
            int[] rowIds = {R.id.row_0, R.id.row_1, R.id.row_2, R.id.row_3, R.id.row_4};
            int[] iconIds = {R.id.icon_0, R.id.icon_1, R.id.icon_2, R.id.icon_3, R.id.icon_4};
            int[] textIds = {R.id.text_0, R.id.text_1, R.id.text_2, R.id.text_3, R.id.text_4};
            int[] scoreIds = {R.id.score_0, R.id.score_1, R.id.score_2, R.id.score_3, R.id.score_4};
            int[] btnIds = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4};

            for (int i = 0; i < 5; i++) {
                if (i < taskList.size()) {
                    JSONObject task = taskList.get(i);
                    views.setViewVisibility(rowIds[i], View.VISIBLE);
                    
                    views.setTextViewText(iconIds[i], task.optString("icon", "📌"));
                    // 加个时间显示更饱满
                    String time = task.optString("time", "");
                    String text = task.optString("text", "");
                    views.setTextViewText(textIds[i], time + " " + text);
                    
                    // 根据是否有 score 字段决定是否显示，这里假设固定写死
                    // views.setTextViewText(scoreIds[i], "+" + task.optInt("score", 1));

                    boolean done = task.optBoolean("done", false);
                    views.setImageViewResource(btnIds[i], done ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);

                    // 设置点击事件
                    Intent toggleIntent = new Intent(context, HealthWidgetProvider.class);
                    toggleIntent.setAction(ACTION_TOGGLE);
                    toggleIntent.putExtra("task_key", task.optString("key", ""));
                    // 不同的行使用不同的 requestCode，防止 PendingIntent 被覆盖
                    PendingIntent togglePi = PendingIntent.getBroadcast(context, 1000 + i, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(rowIds[i], togglePi); // 点击整行也可以打卡

                } else {
                    // 数据不够 5 条，隐藏多余的 View (极其安全的操作)
                    views.setViewVisibility(rowIds[i], View.GONE);
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);

        } catch (Exception e) {
            Log.e(TAG, "updateWidget parse data error: " + e.getMessage());
            // 即使解析失败，也要确保小部件能显示，不要让系统杀掉
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
