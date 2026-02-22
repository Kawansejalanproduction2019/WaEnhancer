package com.wmods.wppenhacer.activities;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class SchedulerActivity extends Activity {

    private LinearLayout containerList;
    private static final int PICK_CONTACT = 1;
    private EditText inputPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showScheduleList();
    }

    private void showScheduleList() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#ECE5DD"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Jadwal Pesan");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#075E54"));
        title.setPadding(0, 0, 0, 40);
        root.addView(title);

        Button btnAdd = new Button(this);
        btnAdd.setText("+ Buat Jadwal Baru");
        btnAdd.setBackground(getRect(Color.parseColor("#25D366"), 15));
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setOnClickListener(v -> showCreationView());
        root.addView(btnAdd);

        containerList = new LinearLayout(this);
        containerList.setOrientation(LinearLayout.VERTICAL);
        root.addView(containerList);

        loadData();
        setContentView(scroll);
    }

    private void loadData() {
        containerList.removeAllViews();
        try {
            android.content.SharedPreferences sp = getSharedPreferences("WaGlobal", MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString("scheduled_messages", "[]"));

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int pos = i;

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(30, 30, 30, 30);
                card.setBackground(getRect(Color.WHITE, 20));
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 30, 0, 0);
                card.setLayoutParams(lp);

                TextView txt = new TextView(this);
                txt.setText("Target: " + o.getString("jid") + "\nPesan: " + o.getString("msg") + "\nWaktu: " + o.getString("time"));
                txt.setTextColor(Color.DKGRAY);
                card.addView(txt);

                Button del = new Button(this);
                del.setText("Hapus");
                del.setBackground(null);
                del.setTextColor(Color.RED);
                del.setOnClickListener(v -> {
                    arr.remove(pos);
                    sp.edit().putString("scheduled_messages", arr.toString()).apply();
                    loadData();
                });
                card.addView(del);
                containerList.addView(card);
            }
        } catch (Exception ignored) {}
    }

    private void showCreationView() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(40, 40, 40, 40);
        scroll.addView(lay);

        Button pick = new Button(this);
        pick.setText("Pilih dari Kontak WhatsApp");
        pick.setOnClickListener(v -> {
            Intent it = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(it, PICK_CONTACT);
        });
        lay.addView(pick);

        inputPhone = new EditText(this);
        inputPhone.setHint("Nomor (62...)");
        lay.addView(inputPhone);

        EditText inputMsg = new EditText(this);
        inputMsg.setHint("Isi Pesan");
        lay.addView(inputMsg);

        DatePicker dp = new DatePicker(this);
        lay.addView(dp);

        TimePicker tp = new TimePicker(this);
        tp.setIs24HourView(DateFormat.is24HourFormat(this));
        lay.addView(tp);

        Button save = new Button(this);
        save.setText("Simpan Jadwal");
        save.setOnClickListener(v -> {
            String jid = inputPhone.getText().toString();
            String msg = inputMsg.getText().toString();
            if (jid.isEmpty() || msg.isEmpty()) return;

            Calendar cal = Calendar.getInstance();
            cal.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), tp.getHour(), tp.getMinute(), 0);

            saveProcess(jid, msg, cal);
            finish();
        });
        lay.addView(save);
        setContentView(scroll);
    }

    private void saveProcess(String jid, String msg, Calendar cal) {
        try {
            int id = (int) System.currentTimeMillis();
            android.content.SharedPreferences sp = getSharedPreferences("WaGlobal", MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString("scheduled_messages", "[]"));
            
            JSONObject n = new JSONObject();
            n.put("id", id);
            n.put("jid", jid);
            n.put("msg", msg);
            n.put("time", cal.getTime().toString());
            arr.put(n);
            sp.edit().putString("scheduled_messages", arr.toString()).apply();

            Intent it = new Intent("com.wmods.wppenhacer.SEND_SCHEDULED");
            it.putExtra("JID", jid);
            it.putExtra("MESSAGE", msg);
            
            PendingIntent pi = PendingIntent.getBroadcast(this, id, it, PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            
            Toast.makeText(this, "Berhasil dijadwalkan!", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private GradientDrawable getRect(int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK_CONTACT && res == RESULT_OK) {
            Cursor c = getContentResolver().query(data.getData(), null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                inputPhone.setText(c.getString(i).replaceAll("[^0-9]", ""));
                c.close();
            }
        }
    }
}
