package com.wmods.wppenhacer.xposed.features.general;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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
                            
                            try {
                                Intent intent1 = new Intent();
                                intent1.setClassName("com.dev4mod.waenhancer", "com.wmods.wppenhacer.activities.SchedulerActivity");
                                intent1.putExtra("JID", currentJid);
                                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent1);
                            } catch (Exception e1) {
                                try {
                                    Intent intent2 = new Intent();
                                    intent2.setClassName("com.wmods.wppenhacer", "com.wmods.wppenhacer.activities.SchedulerActivity");
                                    intent2.putExtra("JID", currentJid);
                                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    activity.startActivity(intent2);
                                } catch (Exception e2) {
                                    Toast.makeText(activity, "Error membuka jadwal: " + e2.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
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
