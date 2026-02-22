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
                Intent waIntent = new Intent("com.wmods.wppenhacer.EXECUTE_SCHEDULE");
                waIntent.setPackage("com.whatsapp");
                waIntent.putExtra("JID", jid);
                waIntent.putExtra("MESSAGE", msg);
                waIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                context.sendBroadcast(waIntent);

                Intent wbIntent = new Intent("com.wmods.wppenhacer.EXECUTE_SCHEDULE");
                wbIntent.setPackage("com.whatsapp.w4b");
                wbIntent.putExtra("JID", jid);
                wbIntent.putExtra("MESSAGE", msg);
                wbIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                context.sendBroadcast(wbIntent);
            }
        } catch (Exception ignored) {}
    }
}
