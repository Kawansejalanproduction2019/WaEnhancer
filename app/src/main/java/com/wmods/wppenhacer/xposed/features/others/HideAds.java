package com.wmods.wppenhacer.xposed.features.customization;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.wmods.wppenhacer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HideAds extends Feature {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HideAds(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hide_ads", true)) return;

        XposedBridge.log("HideAds: Mesin Auto-Swipe Kiri Siap Tempur!");

        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, TextView.BufferType.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                CharSequence text = (CharSequence) param.args[0];
                if (text != null) {
                    String s = text.toString().toLowerCase();
                    TextView tv = (TextView) param.thisObject;

                    if (s.contains("bersponsor") || s.contains("sponsored") || s.contains("promosi")) {
                        Object currentTag = tv.getTag();
                        if ("RADAR_ON".equals(currentTag) || "SKIPPED".equals(currentTag)) {
                            return;
                        }
                        tv.setTag("RADAR_ON");
                        startSwipeRadar(tv);
                    } else {
                        tv.setTag(null);
                    }
                }
            }
        });
    }

    private void startSwipeRadar(final TextView tv) {
        Runnable radarTask = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!"RADAR_ON".equals(tv.getTag()) || !tv.isAttachedToWindow()) {
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
                    int screenWidth = tv.getResources().getDisplayMetrics().widthPixels;
                    
                    while (pageRoot.getParent() instanceof View) {
                        View parent = (View) pageRoot.getParent();
                        if (parent.getWidth() >= screenWidth * 0.9f) {
                            pageRoot = parent;
                            break;
                        }
                        pageRoot = parent;
                    }

                    int[] loc = new int[2];
                    pageRoot.getLocationOnScreen(loc);

                    if (loc[0] >= -30 && loc[0] <= 30) {
                        tv.setTag("SKIPPED");
                        
                        XposedBridge.log("HideAds: Iklan Tepat di Tengah. Eksekusi SWIPE KIRI!");
                        simulateSwipeLeft(tv.getRootView());
                        return;
                    }

                    mainHandler.postDelayed(this, 16);
                } catch (Exception e) {
                    tv.setTag(null);
                }
            }
        };
        mainHandler.post(radarTask);
    }

    private void simulateSwipeLeft(View root) {
        try {
            long downTime = SystemClock.uptimeMillis();
            long eventTime = downTime;
            
            float startX = root.getWidth() * 0.8f;
            float endX = root.getWidth() * 0.2f;
            float y = root.getHeight() / 2.0f;

            MotionEvent down = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, startX, y, 0);
            root.dispatchTouchEvent(down);
            down.recycle();

            int steps = 15;
            for (int i = 1; i <= steps; i++) {
                eventTime += 10;
                float currentX = startX - ((startX - endX) * (i / (float) steps));
                MotionEvent move = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, currentX, y, 0);
                root.dispatchTouchEvent(move);
                move.recycle();
            }

            eventTime += 10;
            MotionEvent up = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, endX, y, 0);
            root.dispatchTouchEvent(up);
            up.recycle();
            
        } catch (Exception ignored) {}
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Ads";
    }
}
