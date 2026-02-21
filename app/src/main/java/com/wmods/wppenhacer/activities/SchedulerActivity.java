package com.wmods.wppenhacer.activities;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.Calendar;

public class SchedulerActivity extends Activity {
    private String targetJid;
    private EditText messageInput;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Button scheduleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        targetJid = getIntent().getStringExtra("JID");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        
        messageInput = new EditText(this);
        messageInput.setHint("Ketik pesan di sini");
        
        datePicker = new DatePicker(this);
        timePicker = new TimePicker(this);
        
        scheduleButton = new Button(this);
        scheduleButton.setText("Simpan Jadwal");
        
        layout.addView(messageInput);
        layout.addView(datePicker);
        layout.addView(timePicker);
        layout.addView(scheduleButton);
        
        setContentView(layout);
        
        scheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleMessage();
            }
        });
    }

    private void scheduleMessage() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                     timePicker.getCurrentHour(), timePicker.getCurrentMinute(), 0);
                     
        String message = messageInput.getText().toString();
        
        Intent intent = new Intent("com.wmods.wppenhacer.SEND_SCHEDULED");
        intent.putExtra("JID", targetJid);
        intent.putExtra("MESSAGE", message);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, 
            (int) System.currentTimeMillis(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        
        Toast.makeText(this, "Pesan berhasil dijadwalkan", Toast.LENGTH_SHORT).show();
        finish();
    }
}
