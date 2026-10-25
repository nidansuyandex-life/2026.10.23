package com.daily.healthlife;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

public class TtsHelper implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private boolean isReady = false;

    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true;
            // 默认设置中文，具体语种在 speak() 中动态设置
            int result = tts.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsHelper", "TTS 语言包缺失或不支持");
            }
        } else {
            Log.e("TtsHelper", "TTS 初始化失败");
        }
    }

    public void speak(String text, String lang) {
        if (!isReady || tts == null) return;

        // 设置语言
        Locale locale = "zh".equals(lang) || "zh-CN".equals(lang) ? Locale.CHINA : Locale.US;
        tts.setLanguage(locale);
        tts.setSpeechRate(0.95f); // 稍微放慢语速

        // 停止当前的朗读，开始新的朗读
        tts.stop();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts-id");
    }

    public void stop() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
