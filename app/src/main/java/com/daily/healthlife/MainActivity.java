package com.daily.healthlife;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
    private Uri mCameraPhotoUri;
    private File mCameraPhotoFile;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    private volatile boolean mDestroyed = false;
    /** 系统文件选择器/相机打开期间为 true：此时 onResume 不得整页重绘 */
    private volatile boolean mFileChooserBusy = false;
    private volatile long mFileChooserTs = 0L;

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
            /** P55：主文档加载失败不再白屏，跳到内置兜底页，数据仍在本机 */
            private void showErrorPage(WebView v){
                try {
                    if (mDestroyed || isFinishing() || isDestroyed()) return;
                    v.post(() -> { try { v.loadUrl("file:///android_asset/error.html"); } catch (Throwable ignored) {} });
                } catch (Throwable ignored) {}
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                try { if (request != null && request.isForMainFrame()) showErrorPage(view); } catch (Throwable ignored) {}
            }
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                try { showErrorPage(view); } catch (Throwable ignored) {}
            }
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                try { if (request != null && request.isForMainFrame()) showErrorPage(view); } catch (Throwable ignored) {}
            }
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

            /* Android 7+ 走这个重载；设置页「打开火山方舟控制台」链接要靠它跳外部浏览器 */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && request != null && request.getUrl() != null) {
                        return shouldOverrideUrlLoading(view, request.getUrl().toString());
                    }
                } catch (Throwable ignored) {}
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
                mFileChooserBusy = true;
                mFileChooserTs = System.currentTimeMillis();
                mCameraPhotoUri = null;
                mCameraPhotoFile = null;
                try {
                    String[] accepts = null;
                    boolean capture = false;
                    try { accepts = fileChooserParams.getAcceptTypes(); } catch (Throwable ignored) {}
                    try { capture = fileChooserParams.isCaptureEnabled(); } catch (Throwable ignored) {}

                    // 按 input 的 accept 归一出 MIME 列表。
                    // 上一版把类型写死成 image/*，导致「从 JSON 导入 / 记账 CSV 导入」只能看到照片。
                    java.util.ArrayList<String> mimes = new java.util.ArrayList<String>();
                    boolean anyType = false, imageType = false;
                    if (accepts != null) {
                        for (String a : accepts) {
                            if (a == null) continue;
                            a = a.trim().toLowerCase(Locale.ROOT);
                            if (a.isEmpty()) continue;
                            if (a.equals("*/*")) { anyType = true; continue; }
                            if (a.startsWith(".")) {
                                String m = mimeForExtension(a);
                                if (m != null) { if (!mimes.contains(m)) mimes.add(m); }
                                else anyType = true;
                                continue;
                            }
                            if (a.indexOf('/') > 0) {
                                if (!mimes.contains(a)) mimes.add(a);
                                if (a.startsWith("image/")) imageType = true;
                            }
                        }
                    }
                    if ((accepts == null || accepts.length == 0) && !capture) anyType = true;
                    final boolean imageOnly = imageType && !anyType && !mimes.isEmpty()
                            && allMimePrefix(mimes, "image/");

                    // 只有明确带 capture 的输入（护肤品「拍照识别」）才开系统相机。
                    // 直接 startActivityForResult，不经 chooser 包装，EXTRA_OUTPUT 不会被吞掉。
                    if (capture && !anyType) {
                        try {
                            File dir = new File(getCacheDir(), "images");
                            if (!dir.exists()) dir.mkdirs();
                            File photo = new File(dir, "camera_" + System.currentTimeMillis() + ".jpg");
                            Uri photoUri = FileProvider.getUriForFile(MainActivity.this,
                                    getPackageName() + ".fileprovider", photo);
                            Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cam.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                            cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            cam.setClipData(android.content.ClipData.newUri(getContentResolver(), "photo", photoUri));
                            try {
                                java.util.List<android.content.pm.ResolveInfo> ris = getPackageManager()
                                        .queryIntentActivities(cam, PackageManager.MATCH_DEFAULT_ONLY);
                                for (android.content.pm.ResolveInfo ri : ris) {
                                    grantUriPermission(ri.activityInfo.packageName, photoUri,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                }
                            } catch (Throwable ignored) {}
                            mCameraPhotoUri = photoUri;
                            mCameraPhotoFile = photo;
                            startActivityForResult(cam, FILE_CHOOSER_RESULT_CODE);
                            return true;
                        } catch (Throwable e) {
                            mCameraPhotoUri = null;
                            mCameraPhotoFile = null;
                            // 相机不可用时落到下面的通用选择器
                        }
                    }

                    // 其余输入框：纯文件选择器，绝不附加相机。
                    // 衣橱/随手记/探店看到图片；备份 JSON、记账 CSV/XLSX 看到全部文件。
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    if (imageOnly) {
                        pick.setType("image/*");
                    } else if (!mimes.isEmpty() && !anyType) {
                        pick.setType(mimes.get(0));
                        pick.putExtra(Intent.EXTRA_MIME_TYPES, mimes.toArray(new String[0]));
                    } else {
                        pick.setType("*/*");
                    }
                    try {
                        pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
                                fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                    } catch (Throwable ignored) {}
                    // 直接启动，不经 createChooser：中转 Activity 会吞掉 Uri 的临时读权限，
                    // 导致 WebView 在 file:// 页面里把照片读成 0 字节。
                    startActivityForResult(pick, FILE_CHOOSER_RESULT_CODE);
                    return true;
                } catch (Exception e) {
                    mUploadMessage = null;
                    mFileChooserBusy = false;
                    try { Toast.makeText(MainActivity.this, "无法打开文件选择器", Toast.LENGTH_SHORT).show(); }
                    catch (Throwable ignored) {}
                    return false;
                }
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    /** input accept 里的扩展名映射成 Android MIME；未知扩展名返回 null，让调用方显示所有文件 */
    private static String mimeForExtension(String ext) {
        if (".json".equals(ext)) return "application/json";
        if (".csv".equals(ext)) return "text/csv";
        if (".txt".equals(ext)) return "text/plain";
        if (".xls".equals(ext)) return "application/vnd.ms-excel";
        if (".xlsx".equals(ext)) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (".jpg".equals(ext) || ".jpeg".equals(ext)) return "image/jpeg";
        if (".png".equals(ext)) return "image/png";
        if (".pdf".equals(ext)) return "application/pdf";
        return null;
    }
    private static boolean allMimePrefix(java.util.List<String> list, String prefix) {
        for (String s : list) { if (s == null || !s.startsWith(prefix)) return false; }
        return true;
    }

    /** 极少数相机 App 只回一个缩略图 Bitmap，落成本地文件再交给 WebView */
    private Uri bitmapToCacheUri(Bitmap bmp) {
        if (bmp == null) return null;
        try {
            File dir = new File(getCacheDir(), "images");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "thumb_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream os = new java.io.FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, os);
            os.flush();
            os.close();
            return Uri.fromFile(f);
        } catch (Throwable ignored) { return null; }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_RESULT_CODE) return;
        if (mUploadMessage == null) { scheduleChooserUnlock(); return; }
        Uri[] raw = null;
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        raw = new Uri[]{ Uri.parse(dataString) };
                    } else if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                        int count = data.getClipData().getItemCount();
                        raw = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            raw[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getExtras() != null && data.getExtras().get("data") instanceof Bitmap) {
                        Uri u = bitmapToCacheUri((Bitmap) data.getExtras().get("data"));
                        if (u != null) raw = new Uri[]{ u };
                    }
                }
                // 相机拍照成功但 data == null（绝大多数机型）：用我们预先给它的那个文件
                if (raw == null && mCameraPhotoFile != null
                        && mCameraPhotoFile.exists() && mCameraPhotoFile.length() > 0) {
                    raw = new Uri[]{ Uri.fromFile(mCameraPhotoFile) };
                }
            }
        } catch (Throwable ignored) {}

        /* ★ 把系统返回的 content:// Uri 落盘成 App 私有文件再交给 WebView。
           file input 只拿到临时读授权，一旦页面重绘、input 节点被替换，
           FileReader 就会读到 0 字节 —— 表现为「选完照片界面跳一下，照片没出现」。 */
        Uri[] results = null;
        if (raw != null) {
            java.util.ArrayList<Uri> list = new java.util.ArrayList<Uri>();
            for (int i = 0; i < raw.length; i++) {
                Uri local = copyPickedUriToCache(raw[i], i);
                list.add(local != null ? local : raw[i]);   // 复制失败时退回原 Uri
            }
            if (!list.isEmpty()) results = list.toArray(new Uri[0]);
        }
        try { mUploadMessage.onReceiveValue(results); } catch (Throwable ignored) {}
        mUploadMessage = null;
        mCameraPhotoUri = null;
        mCameraPhotoFile = null;
        scheduleChooserUnlock();
    }

    /** 给 WebView 派发 change / FileReader 留足时间，之后再允许回前台重绘 */
    private void scheduleChooserUnlock() {
        try {
            if (webView != null) {
                webView.postDelayed(() -> { mFileChooserBusy = false; }, 1500);
                return;
            }
        } catch (Throwable ignored) {}
        mFileChooserBusy = false;
    }

    /** content:// → App 私有缓存文件；保留原文件名后缀（记账 CSV/XLSX 靠后缀判断格式） */
    private Uri copyPickedUriToCache(Uri uri, int index) {
        if (uri == null) return null;
        java.io.InputStream in = null;
        java.io.FileOutputStream out = null;
        File dst = null;
        try {
            File dir = new File(getCacheDir(), "picked");
            if (!dir.exists()) dir.mkdirs();
            String name = displayNameFor(uri);
            if (name == null || name.isEmpty()) name = "pick" + extensionFor(uri);
            name = name.replaceAll("[/\\\\:*?\"<>|]", "_");
            dst = new File(dir, "pick_" + System.currentTimeMillis() + "_" + index + "_" + name);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                in = new java.io.FileInputStream(new File(uri.getPath()));
            } else {
                in = getContentResolver().openInputStream(uri);
            }
            if (in == null) return null;
            out = new java.io.FileOutputStream(dst);
            byte[] buf = new byte[16384];
            int n; long total = 0;
            while ((n = in.read(buf)) > 0) { out.write(buf, 0, n); total += n; }
            out.flush();
            if (total <= 0) { try { dst.delete(); } catch (Throwable ignored) {} return null; }
            return Uri.fromFile(dst);
        } catch (Throwable e) {
            Log.w(TAG, "copy picked uri failed: " + uri, e);
            if (dst != null) { try { dst.delete(); } catch (Throwable ignored) {} }
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
        }
    }

    private String displayNameFor(Uri uri) {
        android.database.Cursor c = null;
        try {
            c = getContentResolver().query(uri,
                    new String[]{ android.provider.OpenableColumns.DISPLAY_NAME }, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Throwable ignored) {} finally {
            try { if (c != null) c.close(); } catch (Throwable ignored) {}
        }
        return null;
    }

    private String extensionFor(Uri uri) {
        String mime = null;
        try { mime = getContentResolver().getType(uri); } catch (Throwable ignored) {}
        if (mime != null) {
            if (mime.contains("png")) return ".png";
            if (mime.contains("webp")) return ".webp";
            if (mime.contains("json")) return ".json";
            if (mime.contains("csv") || mime.contains("text/plain")) return ".csv";
            if (mime.contains("spreadsheet") || mime.contains("excel")) return ".xlsx";
        }
        return ".jpg";
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
        /* ★ 系统相册/文件选择器返回时也会走 onResume。此时执行 merge → render 会把刚
           拿到文件的 <input type=file> 与预览节点整体替换，照片就「不见了」、界面闪一下。*/
        if (mFileChooserBusy) {
            if (System.currentTimeMillis() - mFileChooserTs < 30000L) return;
            mFileChooserBusy = false;   // 异常兜底：选择器结果一直没回来
        }
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
