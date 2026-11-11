package com.daily.healthlife;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.json.JSONObject;

import java.util.Locale;

public class TtsHelper {
    private static final String TAG = "TtsHelper";

    private TextToSpeech tts;
    private final Context appContext;
    private volatile boolean initDone = false;
    private volatile boolean initSuccess = false;

    public TtsHelper(Context context) {
        this.appContext = context.getApplicationContext();
        try {
            tts = new TextToSpeech(appContext, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    initSuccess = true;
                    Log.d(TAG, "TTS init success");
                } else {
                    initSuccess = false;
                    Log.e(TAG, "TTS init failed, status=" + status);
                }
                initDone = true;
            });
            /* P58：部分机型 onInit 回调迟迟不触发（或干脆不触发），导致 initDone 永远为
               false、speak 永远返回 false、英文发音永远无声。加一个看门狗，超时后强制
               把 initDone 置 true，让状态可判断、不再死等。 */
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!initDone) {
                    Log.w(TAG, "TTS init timeout, force mark done");
                    initDone = true;
                }
            }, 5000);
        } catch (Exception e) {
            Log.e(TAG, "TTS create failed", e);
            initDone = true;
            initSuccess = false;
        }
    }

    /** P58：供前端诊断发音失败原因（引擎是否存在、英文/中文语音是否可用） */
    public String getStatusJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("engine", tts != null);
            o.put("initDone", initDone);
            o.put("initSuccess", initSuccess);
            if (tts != null && initDone) {
                int en = tts.isLanguageAvailable(Locale.US);
                int zh = tts.isLanguageAvailable(Locale.SIMPLIFIED_CHINESE);
                o.put("enAvailable", en != TextToSpeech.LANG_MISSING_DATA && en != TextToSpeech.LANG_NOT_SUPPORTED);
                o.put("zhAvailable", zh != TextToSpeech.LANG_MISSING_DATA && zh != TextToSpeech.LANG_NOT_SUPPORTED);
            } else {
                o.put("enAvailable", false);
                o.put("zhAvailable", false);
            }
        } catch (Exception e) {
            try { o.put("error", e.getMessage()); } catch (Exception ignored) {}
        }
        return o.toString();
    }

    /** P58：拉起系统“安装 TTS 语音数据”界面，让用户补装英文语音包（解决英文发音无声） */
    public void installTtsData() {
        try {
            Intent intent = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "installTtsData failed", e);
        }
    }

    public boolean isReady() {
        return initDone && initSuccess;
    }

    /**
     * 返回 true  = 已成功加入播放队列（原生会发声）
     * 返回 false = 引擎未就绪 / 语言缺失 / 调用失败 —— 调用方（JS）应立即走网络 TTS
     */
    public boolean speak(String text, String lang) {
        if (text == null || text.trim().isEmpty()) return false;
        if (tts == null) {
            Log.w(TAG, "speak: tts is null");
            return false;
        }
        if (!initDone) {
            Log.w(TAG, "speak: init not done yet");
            return false;
        }
        if (!initSuccess) {
            Log.w(TAG, "speak: init failed earlier");
            return false;
        }

        Locale locale;
        if (lang == null) {
            locale = Locale.getDefault();
        } else if (lang.startsWith("zh")) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else if (lang.startsWith("en")) {
            locale = Locale.US;
        } else {
            locale = Locale.getDefault();
        }

        try {
            int avail = tts.isLanguageAvailable(locale);
            if (avail == TextToSpeech.LANG_MISSING_DATA
                    || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "language not available, lang=" + lang + " avail=" + avail);
                return false;
            }
            int setResult = tts.setLanguage(locale);
            if (setResult == TextToSpeech.LANG_MISSING_DATA
                    || setResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "setLanguage failed, lang=" + lang + " result=" + setResult);
                return false;
            }
            int speakResult = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null,
                    "tts-" + System.currentTimeMillis());
            if (speakResult != TextToSpeech.SUCCESS) {
                Log.w(TAG, "speak returned " + speakResult);
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "speak exception", e);
            return false;
        }
    }

    /** 设置朗读语速（1.0=正常，0.5=较慢）；由 JS 的 speakWithRate 传入 */
    public void setRate(float rate) {
        try {
            if (tts != null) tts.setSpeechRate(Math.max(0.3f, Math.min(2.0f, rate)));
        } catch (Exception ignored) {}
    }

    public void stop() {
        try {
            if (tts != null) tts.stop();
        } catch (Exception ignored) {}
    }

    public void destroy() {
        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        } catch (Exception ignored) {}
        tts = null;
    }
}
