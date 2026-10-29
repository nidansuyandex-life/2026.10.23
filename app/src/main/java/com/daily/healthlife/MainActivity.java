package com.daily.healthlife;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;
    private static final int FILE_CHOOSER_RESULT_CODE = 100;

    // 接收小组件点击打勾的广播
    private final BroadcastReceiver widgetCheckReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.daily.healthlife.WIDGET_CHECK_SYNC".equals(intent.getAction())) {
                String key = intent.getStringExtra("key");
                boolean done = intent.getBooleanExtra("done", false);
                // 通知 WebView 同步状态
                if (webView != null) {
                    webView.post(() -> webView.evaluateJavascript("window.onWidgetCheckin('" + key + "', " + done + ")", null));
                }
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview); // 确保你的 activity_main.xml 里有这个 ID
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // 注册 JS 接口
        webView.addJavascriptInterface(new JsBridge(this), "AndroidBridge");
        
        // 允许网页弹窗选择文件（修复衣橱上传照片无反应）
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }
                uploadMessage = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html"); // 假设 index.html 在 assets 目录下

        // 注册小组件广播
        IntentFilter filter = new IntentFilter("com.daily.healthlife.WIDGET_CHECK_SYNC");
        registerReceiver(widgetCheckReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        // 处理桌面小部件传过来的跳转路由
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("route")) {
            String route = intent.getStringExtra("route");
            if (webView != null) {
                // 延迟一点执行，确保 WebView 已加载完毕
                webView.postDelayed(() -> webView.evaluateJavascript("window.jumpFromNative('" + route + "')", null), 500);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage != null) {
                Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                uploadMessage.onReceiveValue(results);
                uploadMessage = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(widgetCheckReceiver);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    // ===== JS 交互桥接类 =====
    public class JsBridge {
        private final Context context;

        public JsBridge(Context context) {
            this.context = context;
        }

        // 网页同步数据到桌面小组件
        @JavascriptInterface
        public void syncTimelineData(String jsonStr) {
            SharedPreferences prefs = context.getSharedPreferences(HealthWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(HealthWidgetProvider.KEY_TIMELINE_DATA, jsonStr).apply();

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, HealthWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            if (appWidgetIds.length > 0) {
                for (int id : appWidgetIds) {
                    HealthWidgetProvider.updateAppWidget(context, appWidgetManager, id);
                }
            }
        }

        // 预留的其他接口（防止网页报错）
        @JavascriptInterface
        public void syncWidgetData(String study, String expense) {}
        
        @JavascriptInterface
        public void getWidgetTimelineData() {}
    }
}
