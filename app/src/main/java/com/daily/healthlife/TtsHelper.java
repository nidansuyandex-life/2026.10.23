package com.daily.healthlife;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

public class TtsHelper {
    private TextToSpeech tts;
    private boolean isReady = false;
    
    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                isReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    public boolean isReady() {
        return isReady;
    }

    public void speak(String text, String lang) {
        if (tts != null) {
            if (!isReady) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isReady) speak(text, lang);
                }, 500);
                return;
            }
            Locale locale = ("zh-CN".equals(lang) || "zh".equals(lang)) ? Locale.CHINA : Locale.US;
            int result = tts.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsHelper", "语言不支持: " + lang);
                return;
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
