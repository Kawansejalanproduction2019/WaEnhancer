package com.wmods.wppenhacer.xposed.features.general;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;

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
        if (!WppCore.getPrivBoolean("enable_scheduler", false)) return;

        XposedHelpers.findAndHookMethod(
            "android.app.Application",
            classLoader,
            "onCreate",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.thisObject;
                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context ctx, Intent intent) {
                            try {
                                String jid = intent.getStringExtra("JID");
                                String msg = intent.getStringExtra("MESSAGE");
                                if (jid != null && msg != null && !jid.isEmpty() && !msg.isEmpty()) {
                                    if (!jid.contains("@")) {
                                        jid = jid + "@s.whatsapp.net";
                                    }
                                    Object messageHandler = XposedHelpers.callStaticMethod(
                                        XposedHelpers.findClass("com.whatsapp.MessageHandler", classLoader),
                                        "getInstance"
                                    );
                                    XposedHelpers.callMethod(messageHandler, "sendMessage", jid, msg);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    };
                    context.registerReceiver(receiver, new IntentFilter("com.wmods.wppenhacer.SEND_SCHEDULED"));
                }
            }
        );

        Class<?> HomeClass = WppCore.getHomeActivityClass(classLoader);
        if (HomeClass != null) {
            XposedHelpers.findAndHookMethod(
                HomeClass,
                "onCreateOptionsMenu",
                Menu.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Menu menu = (Menu) param.args[0];
                            final Activity activity = (Activity) param.thisObject;
                            
                            MenuItem scheduleItem = menu.add(0, 0, 0, "Jadwal Pesan");
                            scheduleItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    try {
                                        Intent intent = new Intent("com.waenhancer.SCHEDULER");
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        activity.startActivity(intent);
                                    } catch (Throwable ignored) {
                                    }
                                    return true;
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Message Scheduler";
    }
}
