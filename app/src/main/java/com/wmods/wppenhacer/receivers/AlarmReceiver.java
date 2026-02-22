package com.wmods.wppenhacer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String jid = intent.getStringExtra("JID");
            String msg = intent.getStringExtra("MESSAGE");

            if (jid != null && msg != null) {
                // Tembak langsung ke WhatsApp Normal
                Intent waIntent = new Intent("com.wmods.wppenhacer.EXECUTE_SCHEDULE");
                waIntent.setClassName("com.whatsapp", "com.whatsapp.appwidget.WidgetProvider");
                waIntent.putExtra("JID", jid);
                waIntent.putExtra("MESSAGE", msg);
                waIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                context.sendBroadcast(waIntent);

                // Tembak langsung ke WhatsApp Business
                Intent wbIntent = new Intent("com.wmods.wppenhacer.EXECUTE_SCHEDULE");
                wbIntent.setClassName("com.whatsapp.w4b", "com.whatsapp.appwidget.WidgetProvider");
                wbIntent.putExtra("JID", jid);
                wbIntent.putExtra("MESSAGE", msg);
                wbIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                context.sendBroadcast(wbIntent);
            }
        } catch (Exception ignored) {}
    }
}
