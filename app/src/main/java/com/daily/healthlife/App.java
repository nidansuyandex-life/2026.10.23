package com.daily.healthlife;

import android.app.Application;
import android.content.Context;

/**
 * 应用级入口：保证任何组件（通知点击、桌面小组件、广播）冷启动时，
 * 都先装上全局崩溃兜底，而不必等到 MainActivity 打开。
 */
public class App extends Application {
    private static Context sAppContext;

    public static Context context() { return sAppContext; }

    @Override
    public void onCreate() {
        super.onCreate();
        sAppContext = getApplicationContext();
        try { MainActivity.installCrashGuard(); } catch (Throwable ignored) {}
    }
}
