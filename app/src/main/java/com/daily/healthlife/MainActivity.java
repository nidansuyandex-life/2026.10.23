package com.daily.healthlife;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 确保这个布局文件里有 WebView

        webView = findViewById(R.id.webview); // 确保你的 activity_main.xml 里 WebView 的 id 是 webview
        if (webView == null) {
            Log.e("MainActivity", "找不到 WebView，请检查 activity_main.xml 中的 id");
            return;
        }

        // 设置背景色，防止加载时出现黑屏或白屏闪烁
        webView.setBackgroundColor(Color.parseColor("#f6f7fb"));

        WebSettings settings = webView.getSettings();
        
        // 核心配置，少一个都可能白屏
        settings.setJavaScriptEnabled(true); // 开启 JS
        settings.setDomStorageEnabled(true); // 开启本地存储 (localStorage)
        settings.setAllowFileAccess(true); // 允许读取本地文件
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        // 适配屏幕
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // 允许混合内容 (http 和 https)
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 捕获前端报错，输出到 Logcat
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d("WebViewConsole", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("MainActivity", "页面加载完毕");
            }
        });

        // 添加 JS 桥接
        webView.addJavascriptInterface(new JsBridge(this, webView), "AndroidBridge");

        // 加载本地 assets 目录下的 index.html
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
