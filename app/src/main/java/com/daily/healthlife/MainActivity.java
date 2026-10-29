package com.daily.healthlife;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private JsBridge jsBridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. 加载布局文件
        setContentView(R.layout.activity_main);
        
        // 2. 初始化 WebView
        webView = findViewById(R.id.webview);
        setupWebView();
        
        // 3. 请求权限
        checkPermissions();
        
        // 4. 处理外部跳转意图
        handleJumpIntent(getIntent());
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);           // 开启 JS
        settings.setDomStorageEnabled(true);           // 开启 localStorage
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // 允许本地文件访问跨域资源（很重要：HTML 抓取网络 API 需要）
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 5. 注册 JS 桥接（HTML 中用了多个别名）
        jsBridge = new JsBridge(this, webView);
        webView.addJavascriptInterface(jsBridge, "AndroidBridge");
        webView.addJavascriptInterface(jsBridge, "AndroidFile");
        webView.addJavascriptInterface(jsBridge, "AndroidTTS");
        webView.addJavascriptInterface(jsBridge, "AndroidNotify");
        webView.addJavascriptInterface(jsBridge, "AndroidSpeech");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 拦截外部网页链接，使用系统浏览器打开
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        
        // 6. 加载本地 assets 中的入口文件
        webView.loadUrl("file:///android_asset/index.html");
    }

    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 2001);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2002);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleJumpIntent(intent);
    }

    private void handleJumpIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            if ("healthlife".equals(data.getScheme()) && "jump".equals(data.getHost())) {
                String route = data.getQueryParameter("route");
                if (route != null && webView != null) {
                    webView.postDelayed(() -> webView.evaluateJavascript("window.jumpFromNative && window.jumpFromNative('" + route + "')", null), 500);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (jsBridge != null) {
            jsBridge.destroy();
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
