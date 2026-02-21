package com.wmods.wppenhacer.activities;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
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

    private LinearLayout mainLayout;
    private static final int PICK_CONTACT_REQUEST = 1;
    private EditText numberInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showListLayout();
    }

    private void showListLayout() {
        ScrollView scrollView = new ScrollView(this);
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(40, 40, 40, 40);
        scrollView.addView(mainLayout);

        Button btnAddNew = new Button(this);
        btnAddNew.setText("Buat Jadwal Baru");
        btnAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddLayout(null);
            }
        });
        mainLayout.addView(btnAddNew);

        loadScheduledMessages();
        setContentView(scrollView);
    }

    private void loadScheduledMessages() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("WaGlobal", Context.MODE_PRIVATE);
            String jsonStr = prefs.getString("scheduled_messages", "[]");
            final JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                final int index = i;

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setPadding(20, 20, 20, 20);
                
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                divider.setBackgroundColor(Color.DKGRAY);
                mainLayout.addView(divider);

                TextView txtInfo = new TextView(this);
                txtInfo.setText("Ke: " + obj.getString("jid") + "\nPesan: " + obj.getString("msg") + "\nWaktu: " + obj.getString("time"));
                txtInfo.setTextColor(Color.BLACK);
                itemLayout.addView(txtInfo);

                LinearLayout btnLayout = new LinearLayout(this);
                btnLayout.setOrientation(LinearLayout.HORIZONTAL);

                Button btnDelete = new Button(this);
                btnDelete.setText("Hapus");
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            array.remove(index);
                            getSharedPreferences("WaGlobal", Context.MODE_PRIVATE).edit().putString("scheduled_messages", array.toString()).apply();
                            cancelAlarm(obj.getInt("id"));
                            showListLayout();
                        } catch (Exception ignored) {
                        }
                    }
                });

                btnLayout.addView(btnDelete);
                itemLayout.addView(btnLayout);
                mainLayout.addView(itemLayout);
            }
        } catch (Exception ignored) {
        }
    }

    private void showAddLayout(JSONObject existingObj) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout addLayout = new LinearLayout(this);
        addLayout.setOrientation(LinearLayout.VERTICAL);
        addLayout.setPadding(40, 40, 40, 40);
        scrollView.addView(addLayout);

        numberInput = new EditText(this);
        numberInput.setHint("Nomor Tujuan (Contoh: 628123456)");

        Button btnPickContact = new Button(this);
        btnPickContact.setText("Pilih dari Kontak");
        btnPickContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(pickContact, PICK_CONTACT_REQUEST);
            }
        });

        final EditText messageInput = new EditText(this);
        messageInput.setHint("Isi Pesan");

        final DatePicker datePicker = new DatePicker(this);
        final TimePicker timePicker = new TimePicker(this);

        Button btnSave = new Button(this);
        btnSave.setText("Simpan Jadwal");
        
        addLayout.addView(btnPickContact);
        addLayout.addView(numberInput);
        addLayout.addView(messageInput);
        addLayout.addView(datePicker);
        addLayout.addView(timePicker);
        addLayout.addView(btnSave);

        setContentView(scrollView);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String jid = numberInput.getText().toString().trim();
                    String msg = messageInput.getText().toString();

                    if (jid.isEmpty() || msg.isEmpty()) {
                        Toast.makeText(SchedulerActivity.this, "Data tidak lengkap", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                            timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);

                    int alarmId = (int) System.currentTimeMillis();

                    android.content.SharedPreferences prefs = getSharedPreferences("WaGlobal", Context.MODE_PRIVATE);
                    String jsonStr = prefs.getString("scheduled_messages", "[]");
                    JSONArray array = new JSONArray(jsonStr);

                    JSONObject newObj = new JSONObject();
                    newObj.put("id", alarmId);
                    newObj.put("jid", jid);
                    newObj.put("msg", msg);
                    newObj.put("time", calendar.getTime().toString());
                    array.put(newObj);

                    prefs.edit().putString("scheduled_messages", array.toString()).apply();

                    Intent intent = new Intent("com.wmods.wppenhacer.SEND_SCHEDULED");
                    intent.putExtra("JID", jid);
                    intent.putExtra("MESSAGE", msg);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            SchedulerActivity.this, alarmId, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                    Toast.makeText(SchedulerActivity.this, "Tersimpan", Toast.LENGTH_SHORT).show();
                    showListLayout();
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(numberIndex);
                number = number.replaceAll("[^0-9]", "");
                numberInput.setText(number);
                cursor.close();
            }
        }
    }

    private void cancelAlarm(int alarmId) {
        Intent intent = new Intent("com.wmods.wppenhacer.SEND_SCHEDULED");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
    
    @Override
    public void onBackPressed() {
        finish();
    }
}
