import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class DailyWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 这里可以读取你 App 里的真实数据（比如从 SharedPreferences 读）
        val taskDone = 3 
        val taskTotal = 10

        for (appWidgetId in appWidgetIds) {
            // 1. 加载刚才写的布局
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            // 2. 更新文字内容
            views.setTextViewText(R.id.widget_content, "今日任务：$taskDone/$taskTotal")

            // 3. 更新到桌面
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
