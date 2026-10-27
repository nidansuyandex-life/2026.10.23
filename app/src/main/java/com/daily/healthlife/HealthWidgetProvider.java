package com.daily.healthlife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    // 这里的 Action 必须和 Manifest 里 receiver 的 intent-filter 对应
    private static final String ACTION_CHECKIN = "com.daily.healthlife.WIDGET_CHECKIN";
    
    // 【重要】这里必须换成你 JsBridge 存数据用的 SP 名和 Key
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
        // 监听打卡点击事件
        if (ACTION_CHECKIN.equals(intent.getAction())) {
            try {
                String taskKey = intent.getStringExtra("task_key");
                if (taskKey != null) {
                    // 1. 翻转状态并保存
                    toggleTask(context, taskKey);
                    // 2. 刷新所有小部件
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new android.content.ComponentName(context, HealthWidgetProvider.class));
                    for (int appWidgetId : appWidgetIds) {
                        updateWidget(context, appWidgetManager, appWidgetId);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "onReceive crash prevented: " + e.getMessage());
            }
        }
    }

    // 翻转打卡状态
    private void toggleTask(Context context, String taskKey) {
        try {
            SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            String jsonStr = sp.getString(SP_KEY_DATA, "[]");
            JSONArray array = new JSONArray(jsonStr);
            
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.optString("key").equals(taskKey)) {
                    boolean done = obj.optBoolean("done", false);
                    obj.put("done", !done); // 翻转状态
                    break;
                }
            }
            sp.edit().putString(SP_KEY_DATA, array.toString()).apply();

            // 发送广播通知 MainActivity 让 H5 同步（显式指定包名，Android 8.0+ 必备）
            Intent intent = new Intent("com.daily.healthlife.ACTION_SYNC_TO_H5");
            intent.setPackage(context.getPackageName());
            intent.putExtra("data", array.toString());
            context.sendBroadcast(intent);

        } catch (Exception e) {
            Log.e(TAG, "toggleTask error: " + e.getMessage());
        }
    }

    // 核心渲染方法
    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

        try {
            // 1. 打开APP按钮点击事件
            Intent openIntent = new Intent(context, MainActivity.class);
            PendingIntent openPi = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.tv_open_app, openPi);

            // 2. 读取 SP 数据
            SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            String jsonStr = sp.getString(SP_KEY_DATA, "[]");
            JSONArray array = new JSONArray(jsonStr);
            
            List<JSONObject> taskList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                taskList.add(array.getJSONObject(i));
            }

            // 3. 【智能下沉】未完成在前 (false=0)，完成在后 (true=1)
            Collections.sort(taskList, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    boolean done1 = o1.optBoolean("done", false);
                    boolean done2 = o2.optBoolean("done", false);
                    return Boolean.compare(done1, done2);
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
                    
                    // 拼接时间和任务文字，比如 "07:40 起床"
                    String time = task.optString("time", "");
                    String text = task.optString("text", "");
                    views.setTextViewText(textIds[i], time + " " + text);
                    
                    // 分数（假设 JSON 里有 score 字段，没有就显示默认）
                    views.setTextViewText(scoreIds[i], "+" + task.optInt("score", 1));

                    // 改变打勾圆圈样式
                    boolean done = task.optBoolean("done", false);
                    views.setImageViewResource(btnIds[i], done ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);

                    // 【核心】绑定打卡点击事件
                    Intent toggleIntent = new Intent(context, HealthWidgetProvider.class);
                    toggleIntent.setAction(ACTION_CHECKIN);
                    toggleIntent.putExtra("task_key", task.optString("key", ""));
                    // requestCode 用 1000 + i，防止5行点击事件互相覆盖
                    PendingIntent togglePi = PendingIntent.getBroadcast(context, 1000 + i, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    // 整行都可以点击
                    views.setOnClickPendingIntent(rowIds[i], togglePi); 

                } else {
                    // 数据不足 5 条，隐藏对应行
                    views.setViewVisibility(rowIds[i], View.GONE);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "updateWidget parse data error: " + e.getMessage());
        }
        
        // 无论如何，都要更新小部件，防止显示 "载入失败"
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
