package com.daily.healthlife;

import android.content.Context;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TtsHelper {
    private static final String TAG = "TtsHelper";

    private TextToSpeech tts;
    private volatile boolean initDone = false;
    private volatile boolean initSuccess = false;

    public TtsHelper(Context context) {
        try {
            tts = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    initSuccess = true;
                    Log.d(TAG, "TTS init success");
                } else {
                    initSuccess = false;
                    Log.e(TAG, "TTS init failed, status=" + status);
                }
                initDone = true;
            });
        } catch (Exception e) {
            Log.e(TAG, "TTS create failed", e);
            initDone = true;
            initSuccess = false;
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
