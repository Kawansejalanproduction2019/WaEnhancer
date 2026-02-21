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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class MessageScheduler extends Feature {

    public MessageScheduler(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
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
                            String jid = intent.getStringExtra("JID");
                            String msg = intent.getStringExtra("MESSAGE");
                            if (jid != null && msg != null) {
                                try {
                                    Object messageHandler = XposedHelpers.callStaticMethod(
                                        XposedHelpers.findClass("com.whatsapp.MessageHandler", classLoader),
                                        "getInstance"
                                    );
                                    XposedHelpers.callMethod(messageHandler, "sendMessage", jid, msg);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    };
                    context.registerReceiver(receiver, new IntentFilter("com.wmods.wppenhacer.SEND_SCHEDULED"));
                }
            }
        );

        Class<?> ConversationClass = XposedHelpers.findClassIfExists("com.whatsapp.Conversation", classLoader);
        if (ConversationClass != null) {
            XposedHelpers.findAndHookMethod(
                ConversationClass,
                "onCreateOptionsMenu",
                Menu.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Menu menu = (Menu) param.args[0];
                        Activity activity = (Activity) param.thisObject;
                        
                        MenuItem scheduleItem = menu.add(0, 0, 0, "Jadwalkan Pesan");
                        scheduleItem.setOnMenuItemClickListener(item -> {
                            String currentJid = "";
                            try {
                                currentJid = (String) XposedHelpers.getObjectField(param.thisObject, "contactJid");
                            } catch (Exception ignored) {
                            }
                            
                            Intent intent = new Intent();
                            intent.setClassName("com.wmods.wppenhacer", "com.wmods.wppenhacer.activities.SchedulerActivity");
                            intent.putExtra("JID", currentJid);
                            activity.startActivity(intent);
                            return true;
                        });
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
