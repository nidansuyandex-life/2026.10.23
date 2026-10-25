package com.daily.healthlife;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class JsBridge {
    private final MainActivity act;
    private final WebView webView;
    private final TtsHelper tts;
    private final VoiceRecorder recorder;

    public JsBridge(MainActivity a, WebView w) {
        this.act = a;
        this.webView = w;
        this.tts = new TtsHelper(a);
        this.recorder = new VoiceRecorder(a);
    }

    @JavascriptInterface
    public void speak(String text, String lang) { tts.speak(text, lang); }

    @JavascriptInterface
    public void stopSpeak() { tts.stop(); }

    @JavascriptInterface
    public void startRecognize(int durationSec) {
        act.runOnUiThread(() -> recorder.start(durationSec, new VoiceRecorder.Callback() {
            @Override public void onResult(String text) {
                final String s = text == null ? "" : text;
                act.runOnUiThread(() -> webView.evaluateJavascript(
                        "window.onVoiceResult && window.onVoiceResult(" + jsString(s) + ")", null));
            }
            @Override public void onError(String err) {
                act.runOnUiThread(() -> webView.evaluateJavascript(
                        "window.onVoiceError && window.onVoiceError(" + jsString(err) + ")", null));
            }
        }));
    }

    @JavascriptInterface
    public void stopRecognize() { recorder.stop(); }

    public VoiceRecorder getVoiceRecorder() { return recorder; }

    private static String jsString(String s) {
        if (s == null) return "''";
        StringBuilder sb = new StringBuilder("'");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.append("'").toString();
    }

    // ============ 图片文件存储 ============
    @JavascriptInterface
    public String saveImage(String base64Data, String filename) {
        try {
            byte[] bytes = Base64.decode(base64Data.split(",")[1], Base64.DEFAULT);
            File dir = act.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (!existsDir(dir)) return "";
            File f = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes);
            fos.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            Log.e("JsBridge", "saveImage error", e);
            return "";
        }
    }

    private boolean existsDir(File dir) {
        if (dir == null) return false;
        if (!dir.exists()) return dir.mkdirs();
        return true;
    }

    @JavascriptInterface
    public String readImage(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return "";
            FileInputStream fis = new FileInputStream(f);
            byte[] bytes = new byte[(int) f.length()];
            fis.read(bytes);
            fis.close();
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64;
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public void deleteImage(String path) {
        try {
            File f = new File(path);
            if (f.exists()) f.delete();
        } catch (Exception ignored) {}
    }

    // ============ 文件保存 ============
    @JavascriptInterface
    public void saveFile(String content, String filename, String mime) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                cv.put(MediaStore.Downloads.MIME_TYPE, mime == null ? "application/octet-stream" : mime);
                Uri uri = act.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) throw new Exception("无法创建文件");
                OutputStream os = act.getContentResolver().openOutputStream(uri);
                if (os == null) throw new Exception("无法打开输出流");
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, filename);
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(content.getBytes(StandardCharsets.UTF_8));
                fos.close();
            }
            final String fname = filename;
            act.runOnUiThread(() -> {
                try {
                    new AlertDialog.Builder(act)
                        .setTitle("✓ 文件已保存")
                        .setMessage("文件名：\n" + fname + "\n\n已保存到系统的「下载」文件夹。\n\n查看方式：打开手机自带的「文件管理」APP，\n切换到「分类」或「下载」视图即可找到。")
                        .setPositiveButton("打开下载文件夹", (d, w) -> openDownloadFolder())
                        .setNegativeButton("知道了", null)
                        .show();
                } catch (Exception ex) {
                    Toast.makeText(act, "已保存：" + fname, Toast.LENGTH_LONG).show();
                }
            });
            act.runOnUiThread(() -> webView.evaluateJavascript("window.onFileSaved && window.onFileSaved(true," + jsString(filename) + ")", null));
        } catch (Exception e) {
            Log.e("saveFile", "err", e);
            final String msg = e.getMessage();
            act.runOnUiThread(() -> {
                Toast.makeText(act, "保存失败：" + msg, Toast.LENGTH_LONG).show();
                webView.evaluateJavascript("window.onFileSaved && window.onFileSaved(false," + jsString(msg) + ")", null);
            });
        }
    }

    @JavascriptInterface
    public void openDownloadFolder() {
        act.runOnUiThread(() -> {
            try {
                Intent i = new Intent("android.intent.action.VIEW_DOWNLOADS");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(i); return;
            } catch (Exception ignored) {}
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"), "vnd.android.document/directory");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(i); return;
            } catch (Exception ignored) {}
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_APP_FILES);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(i); return;
            } catch (Exception ignored) {}
            Toast.makeText(act, "请手动打开「文件管理」APP → 分类 → 下载", Toast.LENGTH_LONG).show();
        });
    }

    @JavascriptInterface
    public void notify(String title, String body) {
        try {
            NotificationManager nm = (NotificationManager) act.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel("healthlife", "健康生活提醒", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
            NotificationCompat.Builder b = new NotificationCompat.Builder(act, "healthlife")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);
            nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
        } catch (Exception e) {
            Log.e("notify", "err", e);
        }
    }

    @JavascriptInterface
    public void vibrate(int ms) {
        try {
            Vibrator v = (Vibrator) act.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Exception ignored) {}
    }

    // ============ 后台倒计时 ============
    @JavascriptInterface
    public void startNativeWalkTimer(int seconds) {
        Context context = act.getApplicationContext();
        Intent intent = new Intent(context, WalkTimerService.class);
        intent.putExtra("duration", seconds);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @JavascriptInterface
    public void stopNativeWalkTimer() {
        Context context = act.getApplicationContext();
        Intent intent = new Intent(context, WalkTimerService.class);
        context.stopService(intent);
    }

    // ============ 桌面卡片数据同步 ============
    @JavascriptInterface
    public void syncWidgetData(String studyProgress, String monthExpense) {
        SharedPreferences sp = act.getApplicationContext()
                .getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
        sp.edit()
          .putString("study_progress", studyProgress)
          .putString("month_expense", monthExpense)
          .apply();

        // 触发桌面卡片刷新
        Intent intent = new Intent(act.getApplicationContext(), HealthWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(act.getApplicationContext()).getAppWidgetIds(
                new ComponentName(act.getApplicationContext(), HealthWidgetProvider.class)
        );
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        act.getApplicationContext().sendBroadcast(intent);
    }

    // ============ 桌面打卡状态读取 ============
    @JavascriptInterface
    public boolean getNativeCheckInStatus() {
        SharedPreferences sp = act.getApplicationContext()
                .getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
        return sp.getBoolean("is_checked_in", false);
    }

    public void destroy() {
        tts.destroy();
        recorder.destroy();
    }
}
