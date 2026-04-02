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

        XposedBridge.log("HideAds: Mesin Auto-Skip Radar Murni Siap Tempur!");

        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, TextView.BufferType.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                CharSequence text = (CharSequence) param.args[0];
                if (text != null) {
                    String s = text.toString().toLowerCase();
                    TextView tv = (TextView) param.thisObject;

                    if (s.contains("bersponsor") || s.contains("sponsored") || s.contains("promosi")) {
                        if ("TRACKING".equals(tv.getTag())) return;
                        tv.setTag("TRACKING");
                        startRadar(tv);
                    } else {
                        tv.setTag(null);
                    }
                }
            }
        });
    }

    private void startRadar(final TextView tv) {
        Runnable radarTask = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!"TRACKING".equals(tv.getTag()) || !tv.isAttachedToWindow()) {
                        return;
                    }

                    View pageRoot = tv;
                    for (int i = 0; i < 10; i++) {
                        if (pageRoot.getParent() instanceof View) {
                            View parent = (View) pageRoot.getParent();
                            String parentName = parent.getClass().getName().toLowerCase();
                            if (parentName.contains("recyclerview") || parentName.contains("viewpager")) {
                                break;
                            }
                            pageRoot = parent;
                        } else {
                            break;
                        }
                    }

                    int[] loc = new int[2];
                    pageRoot.getLocationOnScreen(loc);

                    if (loc[0] >= -50 && loc[0] <= 50) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastSkipTime > 800) {
                            lastSkipTime = currentTime;
                            XposedBridge.log("HideAds: [TARGET TERKUNCI] Iklan di depan mata. Tembak Skip!");
                            performSkip();
                        }
                    }

                    mainHandler.postDelayed(this, 100);
                } catch (Exception e) {
                    tv.setTag(null);
                }
            }
        };
        mainHandler.post(radarTask);
    }

    private void performSkip() {
        try {
            Activity activity = WppCore.getCurrentActivity();
            if (activity != null) {
                View decorView = activity.getWindow().getDecorView();
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis() + 20;

                float x = decorView.getWidth() - 10.0f;
                float y = decorView.getHeight() / 2.0f;

                MotionEvent motionEventDown = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
                MotionEvent motionEventUp = MotionEvent.obtain(downTime, eventTime + 20, MotionEvent.ACTION_UP, x, y, 0);

                decorView.dispatchTouchEvent(motionEventDown);
                decorView.dispatchTouchEvent(motionEventUp);

                motionEventDown.recycle();
                motionEventUp.recycle();
            }
        } catch (Exception ignored) {}
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Ads";
    }
}
