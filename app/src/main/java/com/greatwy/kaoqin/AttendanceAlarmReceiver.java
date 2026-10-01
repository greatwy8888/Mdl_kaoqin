package com.greatwy.kaoqin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

public class AttendanceAlarmReceiver extends BroadcastReceiver {
    private static final String PREFS = "kaoqin_notes";
    private static final String PEOPLE_PREFS = "kaoqin_multi_people";
    private static final String ACTION_FILL = "com.greatwy.kaoqin.FILL_NEXT_DAY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            scheduleNextAlarm(context);
            return;
        }
        if (ACTION_FILL.equals(intent.getAction())) {
            fillToday(context);
            scheduleNextAlarm(context);
        }
    }

    public static void fillToday(Context context) {
        Calendar today = Calendar.getInstance();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH) + 1;
        int day = today.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);

        SharedPreferences notes = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        SharedPreferences people = context.getSharedPreferences(PEOPLE_PREFS, Context.MODE_PRIVATE);

        try {
            JSONArray arr = new JSONArray(people.getString("people", "[]"));
            SharedPreferences.Editor e = notes.edit();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                String id = p.optString("id");
                if (id.isEmpty()) continue;

                String key = "multi_note_" + id + "_" + year + "-" + String.format("%02d", month)
                        + "-" + String.format("%02d", day);
                if (notes.getBoolean("multi_manual_" + id + "_" + year + "-" + String.format("%02d", month)
                        + "-" + String.format("%02d", day), false)) continue;

                JSONArray rest = new JSONArray(p.optString("restDays", "[]"));
                boolean isRest = false;
                for (int j = 0; j < rest.length(); j++) {
                    if (rest.optInt(j) == dayOfWeek) {
                        isRest = true;
                        break;
                    }
                }
                e.putString(key, isRest ? "休息" : "正常");
                e.putBoolean("multi_manual_" + id + "_" + year + "-" + String.format("%02d", month)
                        + "-" + String.format("%02d", day), false);
            }
            e.apply();
        } catch (Exception ignored) {
        }
    }

    public static void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AttendanceAlarmReceiver.class);
        intent.setAction(ACTION_FILL);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 1);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(Calendar.getInstance())) next.add(Calendar.DAY_OF_MONTH, 1);

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
    }
}
