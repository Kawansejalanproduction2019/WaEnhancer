package com.wmods.wppenhacer.xposed.features.general;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

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
        Class<?> widgetClass = XposedHelpers.findClassIfExists("com.whatsapp.appwidget.WidgetProvider", classLoader);
        if (widgetClass != null) {
            XposedHelpers.findAndHookMethod(widgetClass, "onReceive", Context.class, Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.args[1];
                    if (intent != null && "com.wmods.wppenhacer.EXECUTE_SCHEDULE".equals(intent.getAction())) {
                        String jid = intent.getStringExtra("JID");
                        String msg = intent.getStringExtra("MESSAGE");
                        
                        if (jid != null && msg != null) {
                            String cleanJid = jid.contains("@") ? jid.split("@")[0] : jid;
                            WppCore.sendMessage(cleanJid, msg);
                        }
                        
                        // Membatalkan eksekusi Widget asli agar WhatsApp tidak error
                        param.setResult(null); 
                    }
                }
            });
        }
    }

    @Override
    public String getPluginName() {
        return "Message Scheduler";
    }
}
