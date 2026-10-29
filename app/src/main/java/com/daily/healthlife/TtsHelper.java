package com.daily.healthlife;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TtsHelper {
    private TextToSpeech tts;
    
    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });
    }

    public void speak(String text, String lang) {
        if (tts != null) {
            if ("zh-CN".equals(lang) || "zh".equals(lang)) {
                tts.setLanguage(Locale.CHINA);
            } else {
                tts.setLanguage(Locale.US);
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
        }
    }

    public void stop() {
        if (tts != null) tts.stop();
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
