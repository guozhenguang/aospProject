package com.example.testvideo;

import android.app.Instrumentation;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.MotionEvent;

public class InjectUtil {

    public static void injectTouch(int action, float x, float y){
        final long now = SystemClock.uptimeMillis();

        MotionEvent event = MotionEvent.obtain(now,now,action,x,y,0);
        InputManager.getInstance().injectInputEvent(event,0);
    }
}
