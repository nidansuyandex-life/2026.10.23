package com.daily.healthlife;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private JsBridge jsBridge;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_CODE = 1001;
    private static final int REQ_PERMS = 1002;

    // 接收桌面小部件的打卡点击事件
    private final BroadcastReceiver widgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (HealthWidgetProvider.ACTION_WIDGET_TOGGLE_ITEM.equals(intent.getAction())) {
                String key = intent.getStringExtra("item_key");
                boolean done = intent.getBooleanExtra("item_done", false);
                
                if (webView != null && key != null) {
                    webView.post(() -> {
                        // 调用 H5 全局方法，同步打卡状态
                        String js = "if(window.onWidgetCheckin) window.onWidgetCheckin('" + key + "', " + done + ");";
                        webView.evaluateJavascript(js, null);
                    });
                }
            }
        }
    };

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 申请录音、通知等权限
        requestPerms();

        // 2. 初始化 WebView
        webView = findViewById(R.id.webview);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setJavaScriptCanOpenWindowsAutomatically(true);

        // 3. 注册 JsBridge 供 H5 调用
        jsBridge = new JsBridge(this, webView);
        webView.addJavascriptInterface(jsBridge, "AndroidBridge");
        webView.addJavascriptInterface(jsBridge, "AndroidTTS");
        webView.addJavascriptInterface(jsBridge, "AndroidFile");
        webView.addJavascriptInterface(jsBridge, "AndroidNotify");

        // 4. 配置 WebViewClient (处理外部链接)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        // 5. 配置 WebChromeClient (处理 H5 文件选择、权限)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
            @Override
            public boolean onShowFileChooser(WebView wv, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = cb;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(Intent.createChooser(i, "选择文件"), FILE_CHOOSER_CODE);
                } catch (Exception e) { filePathCallback = null; return false; }
                return true;
            }
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) { return true; }
        });

        // 6. 加载 H5 页面
        webView.loadUrl("file:///android_asset/index.html");

        // 7. 处理从桌面小部件 "打开APP" 或其他跳转传来的 Intent
        handleIntent(getIntent());

        // 8. 动态注册广播 (Android 13+ 需要指定 RECEIVER_NOT_EXPORTED)
        IntentFilter filter = new IntentFilter(HealthWidgetProvider.ACTION_WIDGET_TOGGLE_ITEM);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(widgetReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(widgetReceiver, filter);
        }
    }

    // 处理单例模式下的新 Intent (如桌面小部件跳转)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    // 解析路由并通知 H5 (healthlife://jump?route=xxx)
    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            String route = intent.getData().getQueryParameter("route");
            if (route != null && webView != null) {
                // 延迟 500ms，确保 WebView 和 H5 页面已加载完毕
                webView.postDelayed(() -> {
                    String js = "if(window.jumpFromNative) window.jumpFromNative('" + route + "');";
                    webView.evaluateJavascript(js, null);
                }, 500);
            }
        }
    }

    // 申请基础权限
    private void requestPerms() {
        String[] perms;
        if (Build.VERSION.SDK_INT >= 33) {
            perms = new String[]{android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.POST_NOTIFICATIONS};
        } else {
            perms = new String[]{android.Manifest.permission.RECORD_AUDIO};
        }
        boolean need = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) { 
                need = true; 
                break; 
            }
        }
        if (need) ActivityCompat.requestPermissions(this, perms, REQ_PERMS);
    }

    // 文件选择回调
    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        if (req == FILE_CHOOSER_CODE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (res == RESULT_OK && data != null && data.getData() != null) {
                    results = new Uri[]{ data.getData() };
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(req, res, data);
    }

    // 拦截返回键：优先关闭 H5 的弹窗，其次网页回退，最后退出 APP
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webView.evaluateJavascript(
                "(function(){ if(document.getElementById('sheet') && document.getElementById('sheet').classList.contains('open')){ closeSheet(); return 'true'; } return 'false'; })()",
                value -> {
                    if (!"\"true\"".equals(value)) {
                        if (webView.canGoBack()) webView.goBack();
                        else finish();
                    }
                });
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 销毁时清理资源和反注册广播
    @Override
    protected void onDestroy() {
        try { 
            unregisterReceiver(widgetReceiver); 
        } catch (Exception ignored) {}
        
        if (jsBridge != null) jsBridge.destroy();
        if (webView != null) { 
            webView.loadUrl("about:blank"); 
            webView.destroy(); 
        }
        super.onDestroy();
    }
}
