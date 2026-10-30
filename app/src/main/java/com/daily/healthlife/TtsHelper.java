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
                // 如果引擎还没准备好，延迟重试
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isReady) speak(text, lang);
                }, 500);
                return;
            }
            
            if ("zh-CN".equals(lang) || "zh".equals(lang)) {
                tts.setLanguage(Locale.CHINA);
            } else {
                tts.setLanguage(Locale.US);
            }
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
