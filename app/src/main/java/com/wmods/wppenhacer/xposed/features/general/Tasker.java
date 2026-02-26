package com.wmods.wppenhacer.xposed.features.general;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.wmods.wppenhacer.xposed.core.Feature;
import com.wmods.wppenhacer.xposed.core.WppCore;
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp;
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator;
import com.wmods.wppenhacer.xposed.utils.Utils;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class Tasker extends Feature {
    private static FMessageWpp fMessage;
    private static boolean taskerEnabled;
    public static String autoSendTarget = null; // Kunci target untuk Ghost Touch Automator

    public Tasker(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        taskerEnabled = prefs.getBoolean("tasker", false);
        if (!taskerEnabled) return;
        hookReceiveMessage();
        registerSenderMessage();
        hookAutoClicker(); // Aktifkan Hantu UI Automator
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Tasker";
    }

    private void hookAutoClicker() {
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                
                // Mencegat saat layar obrolan WA terbuka
                if (activity.getClass().getName().contains("Conversation")) {
                    if (autoSendTarget != null) {
                        autoSendTarget = null; // Hapus target agar tidak terjadi pengulangan
                        XposedBridge.log("Tasker UI Automator: Layar Obrolan Terbuka!");

                        // Jeda 1 detik menunggu animasi WA selesai dan tombol Send muncul
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                int sendId = activity.getResources().getIdentifier("send", "id", activity.getPackageName());
                                View sendBtn = activity.findViewById(sendId);
                                
                                if (sendBtn != null && sendBtn.getVisibility() == View.VISIBLE) {
                                    XposedBridge.log("Tasker UI Automator: Tombol Send Disentuh!");
                                    sendBtn.performClick();
                                    
                                    // Melempar WA kembali ke mode tidur (Background)
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        activity.moveTaskToBack(true);
                                        XposedBridge.log("Tasker UI Automator: Selesai, WA ditidurkan kembali.");
                                    }, 1000);
                                } else {
                                    XposedBridge.log("Tasker UI Automator: Tombol Send tidak ditemukan (Mungkin teks kosong).");
                                }
                            } catch (Exception e) {
                                XposedBridge.log("Tasker UI Automator Crash: " + e.getMessage());
                            }
                        }, 1000); 
                    }
                }
            }
        });
    }

    private void registerSenderMessage() {
        IntentFilter filter = new IntentFilter("com.wmods.wppenhacer.MESSAGE_SENT");
        ContextCompat.registerReceiver(Utils.getApplication(), new SenderMessageBroadcastReceiver(), filter, ContextCompat.RECEIVER_EXPORTED);
    }

    public synchronized static void sendTaskerEvent(String name, String number, String event) {
        if (!taskerEnabled) return;

        Intent intent = new Intent("com.wmods.wppenhacer.EVENT");
        intent.putExtra("name", name);
        intent.putExtra("number", number);
        intent.putExtra("event", event);
        Utils.getApplication().sendBroadcast(intent);
    }

    public void hookReceiveMessage() throws Throwable {
        var method = Unobfuscator.loadReceiptMethod(classLoader);

        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[4] == "sender" || param.args[1] == null || param.args[3] == null)
                    return;
                var fMessage = new FMessageWpp.Key(param.args[3]).getFMessage();
                var userJid = fMessage.getKey().remoteJid;
                var name = WppCore.getContactName(userJid);
                var number = userJid.getPhoneNumber();
                var msg = fMessage.getMessageStr();
                if (TextUtils.isEmpty(msg) || TextUtils.isEmpty(number) || userJid.isStatus())
                    return;
                new Handler(Utils.getApplication().getMainLooper()).post(() -> {
                    Intent intent = new Intent("com.wmods.wppenhacer.MESSAGE_RECEIVED");
                    intent.putExtra("number", number);
                    intent.putExtra("name", name);
                    intent.putExtra("message", msg);
                    Utils.getApplication().sendBroadcast(intent);
                });
            }
        });
    }

    public static class SenderMessageBroadcastReceiver extends BroadcastReceiver {

        @Override
        @SuppressWarnings("deprecation")
        public void onReceive(Context context, Intent intent) {
            XposedBridge.log("Tasker Receiver: Sinyal Eksekusi Diterima!");
            var number = intent.getStringExtra("number");
            if (number == null) {
                number = String.valueOf(intent.getLongExtra("number", 0));
                number = Objects.equals(number, "0") ? null : number;
            }
            var message = intent.getStringExtra("message");
            if (number == null || message == null) return;
            
            final String finalNumber = number.replaceAll("\\D", "");
            autoSendTarget = finalNumber;
            XposedBridge.log("Tasker Receiver: Membangunkan layar dan memanggil UI Automator...");

            try {
                android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    android.os.PowerManager.WakeLock wl = pm.newWakeLock(
                        android.os.PowerManager.FULL_WAKE_LOCK | 
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                        android.os.PowerManager.ON_AFTER_RELEASE, 
                        "WaEnhancer::AutoSend"
                    );
                    wl.acquire(5000);
                }
            } catch (Exception ignored) {}

            // Membuka paksa obrolan WA dengan nomor dan pesan yang sudah siap dikirim
            Intent waIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("whatsapp://send?phone=" + finalNumber + "&text=" + android.net.Uri.encode(message)));
            waIntent.setPackage(context.getPackageName());
            waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(waIntent);
        }
    }
}
