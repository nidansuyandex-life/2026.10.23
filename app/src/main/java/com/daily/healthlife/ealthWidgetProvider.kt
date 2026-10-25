package com.daily.healthlife

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class HealthWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_health_card)

            // 1. 在此处读取 SharedPreferences 或本地数据库的数据
            // 示例演示：
            views.setTextViewText(R.id.widget_progress, "今日学习：3/10")
            views.setTextViewText(R.id.widget_expense, "本月支出：¥128.50")

            // 2. 设置点击卡片启动 App（可以传入 extra 让 WebView 跳转指定 Tab）
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("jump_to", "home") 
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_progress, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_expense, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
