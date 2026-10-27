package com.daily.healthlife;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

// 【关键】所有的内容必须包在这个 class 的大括号里！
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    // 你的 WebView 对象（如果你原本的变量名不叫 webView，请记得改成你的名字）
    private WebView webView;

    // ==========================================
    // 1. 定义接收小部件打卡的广播接收器
    // ==========================================
    private BroadcastReceiver widgetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.daily.healthlife.ACTION_SYNC_TO_H5".equals(intent.getAction())) {
                String newData = intent.getStringExtra("data");
                Log.d(TAG, "收到小部件同步数据: " + newData);
                
                if (webView != null) {
                    // 发送给 H5。
                    // 注意：因为 newData 是个 JSON 字符串，里面可能含有单引号/双引号。
                    // 最安全的方式是用单引号包裹，并对内容里的单引号做转义。
                    // 假设你 H5 里面有个全局函数：window.onWidgetTaskToggled = function(dataStr) { ... }
                    String safeData = newData.replace("'", "\\'"); 
                    String jsCode = "javascript:if(window.onWidgetTaskToggled){window.onWidgetTaskToggled('" + safeData + "')}";
                    webView.evaluateJavascript(jsCode, null);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ----------------------------------------------------
        // 【这里放你原来的代码】
        // setContentView(R.layout.activity_main); 
        // 初始化 webView ...
        // 挂载 JsBridge ...
        // ----------------------------------------------------

        // ==========================================
        // 2. 动态注册广播（必须在 onCreate 里）
        // ==========================================
        IntentFilter filter = new IntentFilter("com.daily.healthlife.ACTION_SYNC_TO_H5");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 及以上
            registerReceiver(widgetReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(widgetReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // ==========================================
        // 3. 注销广播（防止内存泄漏）
        // ==========================================
        try {
            unregisterReceiver(widgetReceiver);
        } catch (Exception e) {
            Log.e(TAG, "注销广播失败: " + e.getMessage());
        }

        // 销毁 webView 等操作...
    }
}
