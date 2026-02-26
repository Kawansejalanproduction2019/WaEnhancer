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
import android.os.Bundle;
import android.provider.ContactsContract;
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

    private LinearLayout listContainer;
    private static final int PICK_CONTACT = 101;
    private EditText phoneInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderScheduleList();
    }

    private void renderScheduleList() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#F5F6F7"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Penjadwal Pesan");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 40);
        root.addView(title);

        Button btnAdd = createBtn("BUAT JADWAL BARU", "#25D366");
        btnAdd.setOnClickListener(v -> renderAddView());
        root.addView(btnAdd);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        loadSavedData();
        setContentView(scroll);
    }

    private void loadSavedData() {
        listContainer.removeAllViews();
        try {
            android.content.SharedPreferences sp = getSharedPreferences("WaGlobal", MODE_PRIVATE);
            JSONArray arr = new JSONArray(sp.getString("scheduled_messages", "[]"));

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                int pos = i;

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(35, 35, 35, 35);
                card.setBackground(getShape(Color.WHITE, 20));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.setMargins(0, 30, 0, 0);
                card.setLayoutParams(lp);

                TextView tv = new TextView(this);
                tv.setText("Tujuan: " + o.getString("jid") + "\nPesan: " + o.getString("msg") + "\nWaktu: " + o.getString("time"));
                tv.setTextColor(Color.parseColor("#455A64"));
                card.addView(tv);

                Button del = new Button(this);
                del.setText("HAPUS");
                del.setTextColor(Color.RED);
                del.setBackground(null);
                del.setOnClickListener(v -> {
                    try {
                        int alarmId = o.getInt("id");
                        arr.remove(pos);
                        sp.edit().putString("scheduled_messages", arr.toString()).apply();

                        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

                        Intent itGlobal = new Intent("com.wmods.wppenhacer.MESSAGE_SENT");
                        PendingIntent piGlobal = PendingIntent.getBroadcast(this, alarmId, itGlobal, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        am.cancel(piGlobal);

                        loadSavedData();
                    } catch (Exception ignored) {}
                });
                card.addView(del);
                listContainer.addView(card);
            }
        } catch (Exception ignored) {}
    }

    private void renderAddView() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(40, 40, 40, 40);
        scroll.addView(form);

        Button pick = createBtn("PILIH KONTAK", "#34B7F1");
        pick.setOnClickListener(v -> {
            startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), PICK_CONTACT);
        });
        form.addView(pick);

        phoneInput = new EditText(this);
        phoneInput.setHint("Nomor HP (62...)");
        form.addView(phoneInput);

        EditText msgIn = new EditText(this);
        msgIn.setHint("Isi Pesan");
        form.addView(msgIn);

        DatePicker dp = new DatePicker(this);
        form.addView(dp);

        TimePicker tp = new TimePicker(this);
        tp.setIs24HourView(true);
        form.addView(tp);

        Button testBtn = createBtn("TEST KIRIM LANGSUNG", "#FF9800");
        testBtn.setOnClickListener(v -> {
            String jid = phoneInput.getText().toString().replaceAll("[^0-9]", "");
            String msg = msgIn.getText().toString();
            if (jid.isEmpty() || msg.isEmpty()) {
                Toast.makeText(this, "Isi nomor dan pesan!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Murni mengikuti Tasker: Global Broadcast tanpa setPackage!
            Intent testIntent = new Intent("com.wmods.wppenhacer.MESSAGE_SENT");
            testIntent.putExtra("number", jid);
            testIntent.putExtra("message", msg);
            testIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(testIntent);

            Toast.makeText(this, "Sinyal Global Tasker ditembakkan!", Toast.LENGTH_SHORT).show();
        });
        form.addView(testBtn);

        Button save = createBtn("SIMPAN JADWAL", "#128C7E");
        save.setOnClickListener(v -> {
            String jid = phoneInput.getText().toString().replaceAll("[^0-9]", "");
            String msg = msgIn.getText().toString();
            if (jid.isEmpty() || msg.isEmpty()) return;

            Calendar cal = Calendar.getInstance();
            cal.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), tp.getHour(), tp.getMinute(), 0);

            saveProcess(jid, msg, cal);
            finish();
        });
        form.addView(save);
        
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

            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

            Intent itGlobal = new Intent("com.wmods.wppenhacer.MESSAGE_SENT");
            itGlobal.putExtra("number", jid);
            itGlobal.putExtra("message", msg);
            itGlobal.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            
            PendingIntent piGlobal = PendingIntent.getBroadcast(this, id, itGlobal, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(cal.getTimeInMillis(), piGlobal), piGlobal);

            Toast.makeText(this, "Jadwal dikunci (Global Tasker Mode)", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private Button createBtn(String t, String c) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextColor(Color.WHITE);
        b.setBackground(getShape(Color.parseColor(c), 12));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 20, 0, 0);
        b.setLayoutParams(params);
        
        return b;
    }

    private GradientDrawable getShape(int c, int r) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(c);
        gd.setCornerRadius(r);
        return gd;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == PICK_CONTACT && res == RESULT_OK) {
            Cursor c = getContentResolver().query(data.getData(), null, null, null, null);
            if (c != null && c.moveToFirst()) {
                String num = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[^0-9]", "");
                phoneInput.setText(num);
                c.close();
            }
        }
    }
}
