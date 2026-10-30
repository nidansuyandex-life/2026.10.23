package com.daily.healthlife;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TtsHelper {
    private TextToSpeech tts;
    private boolean isReady = false;
    
    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isReady = true;
            }
        });
    }

    public void speak(String text, String lang) {
        if (tts != null) {
            if (!isReady) {
                // 如果引擎还未准备好，延迟尝试
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isReady) speak(text, lang);
                }, 500);
                return;
            }
            
            if ("zh-CN".equals(lang) || "zh".equals(lang)) {
                int result = tts.setLanguage(Locale.CHINA);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
            } else {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
            
            // 使用 QUEUE_FLUSH 清空之前未播完的内容
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
        }
    }

    public void stop() {
        if (tts != null && isReady) tts.stop();
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
