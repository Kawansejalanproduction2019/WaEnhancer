package com.wmods.wppenhacer.xposed.features.general;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import android.app.Activity;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View;
import android.graphics.Color;

public class MessageScheduler {
    public static void init(final ClassLoader classLoader) {
        
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

        XposedHelpers.findAndHookMethod(
            "com.whatsapp.Conversation",
            classLoader,
            "onCreate",
            android.os.Bundle.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    Button scheduleBtn = new Button(activity);
                    scheduleBtn.setText("⏰");
                    scheduleBtn.setBackgroundColor(Color.TRANSPARENT);
                    
                    ViewGroup layout = (ViewGroup) activity.findViewById(
                        activity.getResources().getIdentifier("input_layout", "id", "com.whatsapp")
                    );
                    
                    if (layout != null) {
                        layout.addView(scheduleBtn, 0);
                        
                        scheduleBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String currentJid = (String) XposedHelpers.getObjectField(param.thisObject, "contactJid");
                                Intent intent = new Intent();
                                intent.setClassName("com.wmods.wppenhacer", "com.wmods.wppenhacer.activities.SchedulerActivity");
                                intent.putExtra("JID", currentJid);
                                activity.startActivity(intent);
                            }
                        });
                    }
                }
            }
        );
    }
}
