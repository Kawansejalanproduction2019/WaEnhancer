package com.wmods.wppenhacer.xposed.features.general;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import com.wmods.wppenhacer.xposed.core.Feature;
import com.wmods.wppenhacer.xposed.core.WppCore;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class MessageScheduler extends Feature {
    public MessageScheduler(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        XposedHelpers.findAndHookMethod("android.app.Application", classLoader, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.thisObject;
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctx, Intent intent) {
                        String jid = intent.getStringExtra("JID");
                        String msg = intent.getStringExtra("MESSAGE");
                        if (jid != null && msg != null) {
                            WppCore.sendMessage(jid, msg);
                        }
                    }
                }, new IntentFilter("com.wmods.wppenhacer.SEND_SCHEDULED"));
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() { return "Message Scheduler"; }
}
