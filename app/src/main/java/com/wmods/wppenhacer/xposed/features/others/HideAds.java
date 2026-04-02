package com.wmods.wppenhacer.xposed.features.customization;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.wmods.wppenhacer.xposed.core.Feature;
import com.wmods.wppenhacer.xposed.core.WppCore;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HideAds extends Feature {

    private static long lastSkipTime = 0;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HideAds(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hide_ads", true)) return;

        XposedBridge.log("HideAds: Mesin Turbo Auto-Skip Siap Tempur!");

        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, TextView.BufferType.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                CharSequence text = (CharSequence) param.args[0];
                if (text != null) {
                    String s = text.toString().toLowerCase();
                    TextView tv = (TextView) param.thisObject;

                    if (s.contains("bersponsor") || s.contains("sponsored") || s.contains("promosi")) {
                        if ("LOCKED".equals(tv.getTag())) return;
                        tv.setTag("LOCKED");
                        startTurboRadar(tv);
                    } else {
                        tv.setTag(null);
                    }
                }
            }
        });
    }

    private void startTurboRadar(final TextView tv) {
        Runnable radarTask = new Runnable() {
            private boolean isExecuted = false;

            @Override
            public void run() {
                try {
                    if (isExecuted || !"LOCKED".equals(tv.getTag()) || !tv.isAttachedToWindow()) {
                        return;
                    }

                    CharSequence currentText = tv.getText();
                    if (currentText == null) {
                        tv.setTag(null);
                        return;
                    }

                    String check = currentText.toString().toLowerCase();
                    if (!check.contains("bersponsor") && !check.contains("sponsored") && !check.contains("promosi")) {
                        tv.setTag(null);
                        return;
                    }

                    View pageRoot = tv;
                    for (int i = 0; i < 10; i++) {
                        if (pageRoot.getParent() instanceof View) {
                            View parent = (View) pageRoot.getParent();
                            String name = parent.getClass().getName().toLowerCase();
                            if (name.contains("recyclerview") || name.contains("viewpager")) {
                                break;
                            }
                            pageRoot = parent;
                        } else {
                            break;
                        }
                    }

                    int[] loc = new int[2];
                    pageRoot.getLocationOnScreen(loc);

                    if (loc[0] >= -30 && loc[0] <= 30) {
                        long now = System.currentTimeMillis();
                        if (now - lastSkipTime > 500) {
                            lastSkipTime = now;
                            isExecuted = true;
                            tv.setTag(null);
                            
                            XposedBridge.log("HideAds: [TURBO] Iklan Terdeteksi di Tengah. Eksekusi!");
                            performInstantSkip();
                            return;
                        }
                    }

                    mainHandler.postDelayed(this, 16);
                } catch (Exception e) {
                    tv.setTag(null);
                }
            }
        };
        mainHandler.post(radarTask);
    }

    private void performInstantSkip() {
        try {
            Activity activity = WppCore.getCurrentActivity();
            if (activity != null) {
                View decorView = activity.getWindow().getDecorView();
                long downTime = SystemClock.uptimeMillis();
                
                float x = decorView.getWidth() - 5.0f;
                float y = decorView.getHeight() / 2.0f;

                MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
                MotionEvent up = MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_UP, x, y, 0);

                decorView.dispatchTouchEvent(down);
                decorView.dispatchTouchEvent(up);

                down.recycle();
                up.recycle();
            }
        } catch (Exception ignored) {}
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Ads";
    }
}
