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
        return tts != null && tts.isReady();
    }

    /** 返回 true=原生已发声；false=让 JS 走网络 TTS */
    @JavascriptInterface
    public boolean speak(String text, String lang) {
        return tts != null && tts.speak(text, lang);
    }

    @JavascriptInterface
    public void stopSpeak() {
        if (tts != null) tts.stop();
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
                if (os != null) {
                    os.write(content.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
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

    // ==================== 站立提醒：把调度交给系统 ====================

    /**
     * 页面把提醒设置同步过来，原生用 AlarmManager 排下一次。
     * enabled: 1 开 / 0 关；startMin/endMin 为当天的分钟数；freq 为间隔分钟。
     */
    @JavascriptInterface
    public void syncWalkSettings(int enabled, int startMin, int endMin, int freq) {
        try {
            Context ctx = act.getApplicationContext();
            SharedPreferences sp = ctx.getSharedPreferences(WalkAlarmReceiver.PREFS, Context.MODE_PRIVATE);
            sp.edit()
                    .putBoolean("enabled", enabled == 1)
                    .putInt("startMin", startMin)
                    .putInt("endMin", endMin)
                    .putInt("freq", Math.max(15, freq))
                    .apply();
            if (enabled == 1) {
                WalkAlarmReceiver.scheduleNext(ctx);
            } else {
                WalkAlarmReceiver.cancel(ctx);
            }
        } catch (Exception ignored) {
        }
    }

    /** 查询系统里下一次提醒的时间（毫秒时间戳，0 表示未排） */
    @JavascriptInterface
    public String getWalkAlarmInfo() {
        try {
            Context ctx = act.getApplicationContext();
            SharedPreferences sp = ctx.getSharedPreferences(WalkAlarmReceiver.PREFS, Context.MODE_PRIVATE);
            android.app.AlarmManager am = (android.app.AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(ctx, 4301,
                    new Intent(ctx, WalkAlarmReceiver.class).setAction(WalkAlarmReceiver.ACTION_FIRE),
                    android.app.PendingIntent.FLAG_NO_CREATE | android.app.PendingIntent.FLAG_IMMUTABLE);
            boolean pending = pi != null;
            boolean enabled = sp.getBoolean("enabled", true);
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am != null) {
                exact = am.canScheduleExactAlarms();
            }
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("enabled", enabled);
            o.put("scheduled", pending);
            o.put("exact", exact);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    // ==================== 晚间小结提醒 ====================

    /** 页面把晚间提醒设置同步过来；enabled: 1 开 / 0 关，minute 为当天的分钟数（如 20:30 = 1230） */
    @JavascriptInterface
    public void syncEveningSettings(int enabled, int minute) {
        try {
            Context ctx = act.getApplicationContext();
            SharedPreferences sp = ctx.getSharedPreferences(EveningReminderReceiver.PREFS, Context.MODE_PRIVATE);
            sp.edit()
                    .putBoolean("enabled", enabled == 1)
                    .putInt("minute", Math.max(0, Math.min(23 * 60 + 59, minute)))
                    .apply();
            if (enabled == 1) {
                EveningReminderReceiver.scheduleNext(ctx);
            } else {
                EveningReminderReceiver.cancel(ctx);
            }
        } catch (Exception ignored) {
        }
    }

    /** 页面把「今日未完成摘要」写过来（如「今天还有 2 件待办没完成 · 星光还没领哦」），晚间通知直接用它做文案 */
    @JavascriptInterface
    public void saveEveningBrief(String dateYmd, String text) {
        try {
            Context ctx = act.getApplicationContext();
            SharedPreferences sp = ctx.getSharedPreferences(EveningReminderReceiver.PREFS, Context.MODE_PRIVATE);
            sp.edit()
                    .putString("briefDate", dateYmd == null ? "" : dateYmd)
                    .putString("briefText", text == null ? "" : text)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    // ==================== 备份落地到文件 ====================

    private File backupDir() {
        File dir = new File(act.getExternalFilesDir(null), "backups");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** 写一份备份文件，返回绝对路径（失败返回空串） */
    @JavascriptInterface
    public String saveBackupFile(String json, String filename) {
        try {
            if (json == null) return "";
            String name = (filename == null ? "" : filename).replaceAll("[^0-9A-Za-z._-]", "");
            if (name.length() == 0) name = "backup-" + System.currentTimeMillis() + ".json";
            if (!name.endsWith(".json")) name = name + ".json";
            File dir = backupDir();
            File f = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();
            pruneBackups(dir, 6);
            return f.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }

    /** 列出备份文件，按时间倒序：[{name,size,t}] */
    @JavascriptInterface
    public String listBackupFiles() {
        try {
            File[] fs = backupDir().listFiles();
            if (fs == null) return "[]";
            java.util.List<File> list = new java.util.ArrayList<>();
            for (File f : fs) {
                if (f.isFile() && f.getName().endsWith(".json")) list.add(f);
            }
            list.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            org.json.JSONArray arr = new org.json.JSONArray();
            for (File f : list) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("name", f.getName());
                o.put("size", f.length());
                o.put("t", f.lastModified());
                arr.put(o);
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 读回一份备份内容 */
    @JavascriptInterface
    public String readBackupFile(String name) {
        try {
            String n = (name == null ? "" : name).replaceAll("[^0-9A-Za-z._-]", "");
            File f = new File(backupDir(), n);
            if (!f.exists() || !f.isFile()) return "";
            FileInputStream fis = new FileInputStream(f);
            byte[] bytes = new byte[(int) f.length()];
            int read = fis.read(bytes);
            fis.close();
            return new String(bytes, 0, Math.max(read, 0), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    @JavascriptInterface
    public boolean deleteBackupFile(String name) {
        try {
            String n = (name == null ? "" : name).replaceAll("[^0-9A-Za-z._-]", "");
            File f = new File(backupDir(), n);
            return f.exists() && f.delete();
        } catch (Exception e) {
            return false;
        }
    }

    private void pruneBackups(File dir, int keep) {
        try {
            File[] fs = dir.listFiles();
            if (fs == null) return;
            java.util.List<File> list = new java.util.ArrayList<>();
            for (File f : fs) {
                if (f.isFile() && f.getName().endsWith(".json")) list.add(f);
            }
            if (list.size() <= keep) return;
            list.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = keep; i < list.size(); i++) list.get(i).delete();
        } catch (Exception ignored) {
        }
    }

    @JavascriptInterface
    public void vibrate(int ms) {        try {
            Vibrator v = (Vibrator) act.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Exception ignored) {}
    }

    /** 原生 HTTP GET 桥：WebView 内 fetch 被网络环境拦截时，由原生代码发请求，结果通过 window.__nativeHttpCb 回传 */
    @JavascriptInterface
    public void httpGet(String url, String cbId) {
        new Thread(() -> {
            String result;
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36");
                conn.setRequestProperty("Accept", "application/json,text/plain,*/*");
                conn.setRequestProperty("Accept-Encoding", "identity");
                int code = conn.getResponseCode();
                java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                if (is != null) { while ((n = is.read(buf)) > 0) bos.write(buf, 0, n); is.close(); }
                try { conn.disconnect(); } catch (Exception ignored) {}
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("ok", code >= 200 && code < 300);
                o.put("status", code);
                o.put("body", new String(bos.toByteArray(), StandardCharsets.UTF_8));
                result = o.toString();
            } catch (Exception e) {
                try {
                    org.json.JSONObject o = new org.json.JSONObject();
                    o.put("ok", false);
                    o.put("error", e.getMessage() == null ? "请求失败" : e.getMessage());
                    result = o.toString();
                } catch (Exception e2) { result = "{\"ok\":false,\"error\":\"request failed\"}"; }
            }
            final String r = result;
            act.runOnUiThread(() -> webView.evaluateJavascript(
                    "window.__nativeHttpCb && window.__nativeHttpCb(" + org.json.JSONObject.quote(cbId) + "," + r + ")", null));
        }).start();
    }


    /** 原生 HTTP POST JSON 桥：AI 对话 / 拍照识别在 file:// WebView 里会被 CORS 拦，
        由原生代码发请求绕过跨域；headersJson 为 {"Authorization":"Bearer ..."} 形式 */
    @JavascriptInterface
    public void httpPostJson(String url, String json, String headersJson, String cbId) {
        new Thread(() -> {
            String result;
            java.net.HttpURLConnection conn = null;
            try {
                conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(32000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36");
                if (headersJson != null && !headersJson.isEmpty()) {
                    org.json.JSONObject h = new org.json.JSONObject(headersJson);
                    java.util.Iterator<String> it = h.keys();
                    while (it.hasNext()) {
                        String k = it.next();
                        conn.setRequestProperty(k, h.optString(k, ""));
                    }
                }
                try (OutputStream os = conn.getOutputStream()) {
                    os.write((json == null ? "" : json).getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                if (is != null) { while ((n = is.read(buf)) > 0) bos.write(buf, 0, n); is.close(); }
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("ok", code >= 200 && code < 300);
                o.put("status", code);
                o.put("body", new String(bos.toByteArray(), StandardCharsets.UTF_8));
                result = o.toString();
            } catch (Exception e) {
                try {
                    org.json.JSONObject o = new org.json.JSONObject();
                    o.put("ok", false);
                    o.put("error", e.getMessage() == null ? "请求失败" : e.getMessage());
                    result = o.toString();
                } catch (Exception e2) { result = "{\"ok\":false,\"error\":\"request failed\"}"; }
            } finally {
                if (conn != null) { try { conn.disconnect(); } catch (Exception ignored) {} }
            }
            final String r = result;
            act.runOnUiThread(() -> webView.evaluateJavascript(
                    "window.__nativeHttpCb && window.__nativeHttpCb(" + org.json.JSONObject.quote(cbId) + "," + r + ")", null));
        }).start();
    }

    /** 设置页「发一条测试」：立即发一条通知，验证通知权限与渠道是否正常 */
    @JavascriptInterface
    public void testEveningNotification() {
        try {
            Context ctx = act.getApplicationContext();
            EveningReminderReceiver.sendTestNotification(ctx);
        } catch (Exception ignored) {}
    }

    public void destroy() {
        tts.destroy();
        recorder.destroy();
    }
}
