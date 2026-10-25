package com.daily.healthlife

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences("WidgetData", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("data", null)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            val date = SimpleDateFormat("MM月dd日", Locale.CHINA).format(Date())
            views.setTextViewText(R.id.widget_date, date)

            if (jsonStr != null) {
                try {
                    val json = JSONObject(jsonStr)
                    views.setTextViewText(R.id.widget_study, json.optString("study", "0/10"))
                    views.setTextViewText(R.id.widget_sport, json.optString("sport", "—"))
                    views.setTextViewText(R.id.widget_money, json.optString("money", "¥0"))
                    views.setTextViewText(R.id.widget_timeline_1, json.optString("timeline1", "暂无待办"))
                    views.setTextViewText(R.id.widget_timeline_2, json.optString("timeline2", ""))
                } catch (e: Exception) { }
            }

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_timeline_1, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_timeline_2, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
