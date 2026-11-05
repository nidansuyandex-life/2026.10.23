package com.daily.healthlife;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebView webView;
    private JsBridge jsBridge;
    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    private volatile boolean mDestroyed = false;

    /** 供 WalkAlarmReceiver 判断 App 是否在前台、以及直接调用页面里的方法 */
    private static volatile boolean sForeground = false;
    private static java.lang.ref.WeakReference<MainActivity> sInstance =
            new java.lang.ref.WeakReference<>(null);

    public static boolean isAppForeground() {
        return sForeground;
    }

    /**
     * 在页面里执行一段 JS，返回是否成功派发。
     * ★ 关键修复：Activity 已结束 / WebView 已 destroy 时静默跳过，且异常全部吞掉。
     *   微信来消息时系统可能回收 Activity，此时异步回调（网络、语音、闹钟）
     *   若直接 evaluateJavascript 会抛异常并杀掉进程。
     */
    public static boolean runJs(String script) {
        MainActivity a = sInstance.get();
        if (a == null || a.webView == null || script == null) return false;
        if (a.mDestroyed || a.isFinishing() || a.isDestroyed()) return false;
        try {
            a.runOnUiThread(() -> {
                try {
                    MainActivity cur = sInstance.get();
                    if (cur == null || cur.webView == null) return;
                    if (cur.mDestroyed || cur.isFinishing() || cur.isDestroyed()) return;
                    cur.webView.evaluateJavascript(script, null);
                } catch (Throwable ignored) {}
            });
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        installCrashGuard();   // 与 App.onCreate 双保险，内部幂等
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        sInstance = new java.lang.ref.WeakReference<>(this);
        setupWebView();
        try { checkPermissions(); } catch (Throwable t) { Log.e(TAG, "permission failed", t); }
        // 装机后第一次打开就排上晚间提醒（页面 JS 未必进过设置页）
        try { EveningReminderReceiver.scheduleNext(getApplicationContext()); } catch (Throwable ignored) {}
        try { handleJumpIntent(getIntent()); } catch (Throwable ignored) {}
    }

    /**
     * 全局崩溃兜底。
     * JS 桥方法运行在 WebView 的「JavaBridge」线程（非主线程），那里抛出的
     * ForegroundServiceStartNotAllowedException / SecurityException 会被 Android
     * 默认处理器直接 killProcess —— 这就是「闪退」。
     * 非主线程异常统一只记录、不杀进程；主线程仍交给系统默认处理，避免黑屏 ANR。
     */
    public static void installCrashGuard() {
        try {
            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            if (prev instanceof SafeUncaughtHandler) return;
            Thread.setDefaultUncaughtExceptionHandler(new SafeUncaughtHandler(prev));
        } catch (Throwable ignored) {}
    }

    private static class SafeUncaughtHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler prev;
        SafeUncaughtHandler(Thread.UncaughtExceptionHandler p) { prev = p; }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            try {
                Log.e(TAG, "Uncaught exception in thread " + thread.getName(), ex);
                writeCrashLog(thread, ex);
            } catch (Throwable ignored) {}

            boolean fatal = (ex instanceof OutOfMemoryError) || (ex instanceof StackOverflowError);
            boolean isMain = Looper.getMainLooper().getThread() == thread;
            if (fatal || isMain) {
                if (prev != null) prev.uncaughtException(thread, ex);
            } else {
                // 非主线程：只记录，保住进程不闪退
                Log.w(TAG, "Non-main thread exception swallowed, app keeps running: " + ex);
            }
        }

        private void writeCrashLog(Thread thread, Throwable ex) {
            try {
                MainActivity a = sInstance.get();
                android.content.Context ctx = (a != null) ? a : App.context();
                if (ctx == null) return;
                File dir = ctx.getExternalFilesDir(null);
                if (dir == null) return;
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
                File f = new File(dir, "crash-log.txt");
                FileOutputStream fos = new FileOutputStream(f, true);
                String line = "\n===== " + stamp + " / " + thread.getName() + " =====\n" + sw + "\n";
                fos.write(line.getBytes("UTF-8"));
                fos.close();
            } catch (Throwable ignored) {}
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 允许自动播放媒体，解决音频被拦截问题
        settings.setMediaPlaybackRequiresUserGesture(false);

        // 允许混合内容 (解决部分网络 TTS 接口因 HTTP/HTTPS 冲突被拦截的问题)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        jsBridge = new JsBridge(this, webView);
        webView.addJavascriptInterface(jsBridge, "AndroidBridge");
        webView.addJavascriptInterface(jsBridge, "AndroidFile");
        webView.addJavascriptInterface(jsBridge, "AndroidTTS");
        webView.addJavascriptInterface(jsBridge, "AndroidNotify");
        webView.addJavascriptInterface(jsBridge, "AndroidSpeech");
        webView.addJavascriptInterface(jsBridge, "AndroidHttp");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后，合并桌面小组件的打卡状态
                try {
                    view.postDelayed(() -> {
                        try { MainActivity.runJs("window.mergeNativeWidgetCheckins && window.mergeNativeWidgetCheckins()"); }
                        catch (Throwable ignored) {}
                    }, 250);
                } catch (Throwable ignored) {}
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
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

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mUploadMessage != null) {
                    try { mUploadMessage.onReceiveValue(null); } catch (Throwable ignored) {}
                }
                mUploadMessage = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "选择图片"), FILE_CHOOSER_RESULT_CODE);
                    return true;
                } catch (Exception e) {
                    mUploadMessage = null;
                    try { Toast.makeText(MainActivity.this, "无法打开文件选择器", Toast.LENGTH_SHORT).show(); }
                    catch (Throwable ignored) {}
                    return false;
                }
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (mUploadMessage == null) return;
            Uri[] results = null;
            try {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    }
                }
            } catch (Throwable ignored) {}
            try { mUploadMessage.onReceiveValue(results); } catch (Throwable ignored) {}
            mUploadMessage = null;
        }
    }

    public void checkPermissions() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 2001);
            }
        } catch (Throwable ignored) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2002);
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sForeground = true;
        // 回到前台时校正一次闹钟（设备重启、改过系统时间、切过时区都能修正）
        try { WalkAlarmReceiver.scheduleNext(getApplicationContext()); } catch (Throwable ignored) {}
        // 每次回到前台，同步一次小组件打卡状态
        try { MainActivity.runJs("window.mergeNativeWidgetCheckins && window.mergeNativeWidgetCheckins()"); }
        catch (Throwable ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        sForeground = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        sForeground = false;   // 微信弹通知/下拉通知栏时更准确地反映「非前台」
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try { handleJumpIntent(intent); } catch (Throwable ignored) {}
    }

    private void handleJumpIntent(Intent intent) {
        // 处理来自桌面小组件的点击跳转
        if (intent == null || intent.getData() == null) return;
        Uri data = intent.getData();
        if ("healthlife".equals(data.getScheme()) && "jump".equals(data.getHost())) {
            final String route = data.getQueryParameter("route");
            if (route != null) {
                // 用 JSON quote 生成安全 JS 字符串，避免特殊字符破坏脚本
                final String js = "window.jumpFromNative && window.jumpFromNative(" + org.json.JSONObject.quote(route) + ")";
                try {
                    if (webView != null) {
                        webView.postDelayed(() -> { try { MainActivity.runJs(js); } catch (Throwable ignored) {} }, 500);
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    // 处理物理返回键：优先交由 H5 处理，H5 处理不了再退出 App
    @Override
    public void onBackPressed() {
        if (webView == null || mDestroyed || isFinishing() || isDestroyed()) {
            finish();
            return;
        }
        try {
            webView.evaluateJavascript("window.handleNativeBack ? window.handleNativeBack() : 'false'", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    try {
                        // 注意：evaluateJavascript 返回的字符串如果是 false，会带双引号
                        if ("\"false\"".equals(value) || "false".equals(value) || value == null) {
                            finish();
                        }
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable t) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        mDestroyed = true;
        sForeground = false;
        try { if (jsBridge != null) jsBridge.destroy(); } catch (Throwable ignored) {}
        jsBridge = null;
        if (webView != null) {
            try {
                // 先断开页面里的定时器/桥调用，再销毁，避免销毁后仍有异步回调打到 WebView
                webView.removeJavascriptInterface("AndroidBridge");
                webView.removeJavascriptInterface("AndroidFile");
                webView.removeJavascriptInterface("AndroidTTS");
                webView.removeJavascriptInterface("AndroidNotify");
                webView.removeJavascriptInterface("AndroidSpeech");
                webView.removeJavascriptInterface("AndroidHttp");
            } catch (Throwable ignored) {}
            try { webView.stopLoading(); } catch (Throwable ignored) {}
            try { webView.loadUrl("about:blank"); } catch (Throwable ignored) {}
            try { webView.removeAllViews(); } catch (Throwable ignored) {}
            try { webView.destroy(); } catch (Throwable ignored) {}
            webView = null;
        }
        sInstance = new java.lang.ref.WeakReference<>(null);
        super.onDestroy();
    }
}
