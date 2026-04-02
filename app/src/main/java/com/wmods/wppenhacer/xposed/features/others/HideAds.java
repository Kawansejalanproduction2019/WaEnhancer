package com.wmods.wppenhacer.xposed.features.customization;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

    public HideAds(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hide_ads", true)) return;

        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, TextView.BufferType.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                CharSequence text = (CharSequence) param.args[0];
                if (text != null) {
                    String s = text.toString().toLowerCase();
                    TextView tv = (TextView) param.thisObject;

                    if (s.contains("bersponsor") || s.contains("sponsored") || s.contains("promosi")) {
                        if ("LOCKED_TARGET".equals(tv.getTag())) return;
                        tv.setTag("LOCKED_TARGET");
                        executeMultiScheme(tv);
                    } else {
                        tv.setTag(null);
                        clearRecycledTags(tv);
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(View.class, "setVisibility", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                if ("META_AD_LIST_ITEM".equals(view.getTag())) {
                    param.args[0] = View.GONE;
                }
            }
        });

        XposedHelpers.findAndHookMethod(View.class, "onMeasure", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                if ("META_AD_LIST_ITEM".equals(view.getTag())) {
                    param.args[0] = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY);
                    param.args[1] = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY);
                    param.setResult(null);
                }
            }
        });
    }

    private void clearRecycledTags(View view) {
        try {
            View current = view;
            for (int i = 0; i < 8; i++) {
                if (current.getParent() instanceof View) {
                    current = (View) current.getParent();
                    if ("META_AD_LIST_ITEM".equals(current.getTag())) {
                        current.setTag(null);
                    }
                } else {
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void executeMultiScheme(final TextView tv) {
        try {
            View current = tv;
            boolean isFullScreen = false;
            View targetRoot = null;

            for (int i = 0; i < 8; i++) {
                if (current.getParent() instanceof View) {
                    current = (View) current.getParent();
                    if (current.getHeight() > 1000) {
                        isFullScreen = true;
                        targetRoot = current;
                    }
                } else {
                    break;
                }
            }

            if (isFullScreen && targetRoot != null) {
                final View finalRoot = targetRoot;
                final Handler mainHandler = new Handler(Looper.getMainLooper());

                Runnable skipTask = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            CharSequence currentText = tv.getText();
                            if (currentText == null) {
                                tv.setTag(null);
                                return;
                            }
                            
                            String checkString = currentText.toString().toLowerCase();
                            if (!checkString.contains("bersponsor") && !checkString.contains("sponsored") && !checkString.contains("promosi")) {
                                tv.setTag(null);
                                return;
                            }

                            int[] location = new int[2];
                            finalRoot.getLocationOnScreen(location);

                            if (location[0] >= -50 && location[0] <= 50) {
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastSkipTime < 800) {
                                    mainHandler.postDelayed(this, 100);
                                    return;
                                }
                                lastSkipTime = currentTime;

                                long downTime = SystemClock.uptimeMillis();
                                long eventTime = SystemClock.uptimeMillis() + 20;
                                
                                View clickTarget = finalRoot;
                                try {
                                    Activity activity = WppCore.getCurrentActivity();
                                    if (activity != null) {
                                        clickTarget = activity.getWindow().getDecorView();
                                    }
                                } catch (Exception ignored) {}

                                float x = clickTarget.getWidth() - 20.0f;
                                float y = clickTarget.getHeight() / 2.0f;

                                MotionEvent motionEventDown = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
                                MotionEvent motionEventUp = MotionEvent.obtain(downTime, eventTime + 20, MotionEvent.ACTION_UP, x, y, 0);

                                clickTarget.dispatchTouchEvent(motionEventDown);
                                clickTarget.dispatchTouchEvent(motionEventUp);

                                motionEventDown.recycle();
                                motionEventUp.recycle();

                                tv.setTag(null);
                            } else {
                                mainHandler.postDelayed(this, 100);
                            }
                        } catch (Exception e) {
                            tv.setTag(null);
                        }
                    }
                };

                mainHandler.post(skipTask);

            } else {
                current = tv;
                for (int i = 0; i < 8; i++) {
                    if (current.getParent() instanceof View) {
                        current = (View) current.getParent();
                        current.setTag("META_AD_LIST_ITEM");
                        current.setVisibility(View.GONE);

                        ViewGroup.LayoutParams params = current.getLayoutParams();
                        if (params != null) {
                            params.height = 0;
                            params.width = 0;
                            if (params instanceof ViewGroup.MarginLayoutParams) {
                                ((ViewGroup.MarginLayoutParams) params).setMargins(0, 0, 0, 0);
                            }
                            current.setLayoutParams(params);
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Ads";
    }
}
