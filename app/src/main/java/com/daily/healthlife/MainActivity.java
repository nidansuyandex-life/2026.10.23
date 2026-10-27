// 在 MainActivity.java 里声明广播接收器
private BroadcastReceiver widgetReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.daily.healthlife.ACTION_SYNC_TO_H5".equals(intent.getAction())) {
            String newData = intent.getStringExtra("data");
            // 调用 WebView 的 JS 方法，把新数据传过去，比如：
            if (webView != null) {
                // 假设你的 H5 里有这样一个函数接收数据
                String jsCode = "javascript:onWidgetTaskToggled(" + newData + ")"; 
                webView.evaluateJavascript(jsCode, null);
            }
        }
    }
};

// 记得在 Activity 的 onCreate 里注册它
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... 你的其他代码 ...
    // 注册广播
    IntentFilter filter = new IntentFilter("com.daily.healthlife.ACTION_SYNC_TO_H5");
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(widgetReceiver, filter, Context.RECEIVER_EXPORTED);
    } else {
        registerReceiver(widgetReceiver, filter);
    }
}

// 记得在 onDestroy 里注销，防止内存泄漏
@Override
protected void onDestroy() {
    super.onDestroy();
    try {
        unregisterReceiver(widgetReceiver);
    } catch (Exception e) {
        // 忽略未注册的错误
    }
}
