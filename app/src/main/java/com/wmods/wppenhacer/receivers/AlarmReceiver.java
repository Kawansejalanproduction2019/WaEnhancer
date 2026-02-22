package com.wmods.wppenhacer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String jid = intent.getStringExtra("JID");
            String msg = intent.getStringExtra("MESSAGE");

            if (jid != null && msg != null) {
                Toast.makeText(context, "Sinyal Jadwal Terpicu!", Toast.LENGTH_SHORT).show();
                
                Intent broadcast = new Intent("com.wmods.wppenhacer.EXECUTE_SCHEDULE");
                broadcast.putExtra("JID", jid);
                broadcast.putExtra("MESSAGE", msg);
                broadcast.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                context.sendBroadcast(broadcast);
            }
        } catch (Exception ignored) {}
    }
}
