package com.daily.healthlife;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class HealthWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            // 1. 加载我们在 res/layout 下写好的卡片布局
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_health_card);

            // 2. 读取数据（这里演示从 SharedPreferences 读取，你可以根据实际情况替换）
            // 你的 JsBridge 可以往 SharedPreferences 里存数据，这里就能读到
            SharedPreferences sp = context.getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
            String studyText = sp.getString("study_progress", "今日学习：0/10");
            String expenseText = sp.getString("month_expense", "本月支出：¥0.00");

            views.setTextViewText(R.id.widget_progress, studyText);
            views.setTextViewText(R.id.widget_expense, expenseText);

            // 3. 设置点击事件，点击卡片打开 App
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_progress, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_expense, pendingIntent);

            // 4. 更新卡片
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
