package com.example.testvideo;


import android.app.Application;
import android.content.Context;
import android.content.Intent;

/**
 * Created by BF100165 on 2018/6/21.
 */

public class RecordApplication extends Application {

    private static RecordApplication application;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        application = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 启动 Marvel service

    }

    public static RecordApplication getInstance() {
        return application;
    }
}