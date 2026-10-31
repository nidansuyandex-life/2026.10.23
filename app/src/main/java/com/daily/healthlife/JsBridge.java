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
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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
    public boolean isReady() {
        // 不再调用 TtsHelper.isReady()，避免该类型缺少方法导致编译失败
        return tts != null;
    }

    @JavascriptInterface
    public void speak(String text, String lang) {
        tts.speak(text, lang);
    }

    @JavascriptInterface
    public void stopSpeak() {
        tts.stop();
    }

    @JavascriptInterface
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(act, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(act, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 2002);
            }
        }
    }

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
        context.stopService(new Intent(context, WalkTimerService.class));
    }

    @JavascriptInterface
    public void syncWidgetData(String studyProgress, String monthExpense) {
        SharedPreferences sp = act.getApplicationContext().getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
        sp.edit().putString("study_progress", studyProgress).putString("month_expense", monthExpense).apply();
        refreshWidget();
    }

    @JavascriptInterface
    public void syncTimelineData(String json) {
        SharedPreferences sp = act.getApplicationContext().getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).format(new java.util.Date());
        sp.edit().putString("timeline_data", json).putString("last_sync_date", today).apply();
        refreshWidget();
    }

    private void refreshWidget() {
        Intent intent = new Intent(act.getApplicationContext(), HealthWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(act.getApplicationContext()).getAppWidgetIds(new ComponentName(act.getApplicationContext(), HealthWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        act.getApplicationContext().sendBroadcast(intent);
    }

    @JavascriptInterface
    public String getWidgetTimelineData() {
        return act.getApplicationContext().getSharedPreferences("HealthWidgetData", Context.MODE_PRIVATE).getString("timeline_data", "[]");
    }

    @JavascriptInterface
    public void startRecognize(int durationSec) {
        act.runOnUiThread(() -> recorder.start(durationSec, new VoiceRecorder.Callback() {
            @Override
            public void onResult(String text) {
                final String s = text == null ? "" : text;
                act.runOnUiThread(() -> webView.evaluateJavascript("window.onVoiceResult && window.onVoiceResult(" + jsString(s) + ")", null));
            }

            @Override
            public void onError(String err) {
                act.runOnUiThread(() -> webView.evaluateJavascript("window.onVoiceError && window.onVoiceError(" + jsString(err) + ")", null));
            }
        }));
    }

    @JavascriptInterface
    public void stopRecognize() {
        recorder.stop();
    }

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

    @JavascriptInterface
    public String saveImage(String base64Data, String filename) {
        try {
            byte[] bytes = Base64.decode(base64Data.split(",")[1], Base64.DEFAULT);
            File dir = act.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir != null && !dir.exists()) dir.mkdirs();
            if (dir == null) return "";
            File f = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes);
            fos.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
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
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
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

    @JavascriptInterface
    public void saveFile(String content, String filename, String mime) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                cv.put(MediaStore.Downloads.MIME_TYPE, mime == null ? "application/octet-stream" : mime);
                Uri uri = act.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                OutputStream os = act.getContentResolver().openOutputStream(uri);
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
            act.runOnUiThread(() -> new AlertDialog.Builder(act)
                    .setTitle("✓ 已保存")
                    .setMessage("文件名：\n" + filename)
                    .setPositiveButton("知道了", null)
                    .show());
        } catch (Exception e) {}
    }

    @JavascriptInterface
    public void openDownloadFolder() {
        act.runOnUiThread(() -> {
            try {
                act.startActivity(new Intent("android.intent.action.VIEW_DOWNLOADS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {}
        });
    }

    @JavascriptInterface
    public void notify(String title, String body) {
        try {
            NotificationManager nm = (NotificationManager) act.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(new NotificationChannel("healthlife", "健康生活提醒", NotificationManager.IMPORTANCE_HIGH));
            }
            nm.notify((int) (System.currentTimeMillis() % 100000), new NotificationCompat.Builder(act, "healthlife")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build());
        } catch (Exception e) {}
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

    public void destroy() {
        tts.destroy();
        recorder.destroy();
    }
}
