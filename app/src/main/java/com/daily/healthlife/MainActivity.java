package com.daily.healthlife;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ... 你的 WebView 初始化代码 (setContentView, 设置 JsBridge 等)
        
        handleJumpIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleJumpIntent(intent);
    }

    private void handleJumpIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            // 判断是否是我们的 healthlife://jump 协议
            if ("healthlife".equals(data.getScheme()) && "jump".equals(data.getHost())) {
                String route = data.getQueryParameter("route");
                if (route != null && webView != null) {
                    // 延迟一点执行，确保 H5 已经完全加载
                    webView.postDelayed(() -> 
                        webView.evaluateJavascript("window.jumpFromNative && window.jumpFromNative('" + route + "')", null)
                    , 500); 
                }
            }
        }
    }
}
