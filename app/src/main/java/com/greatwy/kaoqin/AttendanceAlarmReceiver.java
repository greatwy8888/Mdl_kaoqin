package com.greatwy.kaoqin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public class AttendanceAlarmReceiver extends BroadcastReceiver {
    private static final String PREFS = "kaoqin_notes";
    private static final String MANUAL_PREFIX = "manual_";
    private static final String ACTION_FILL = "com.greatwy.kaoqin.FILL_NEXT_DAY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            scheduleNextAlarm(context);
            return;
        }

        if (ACTION_FILL.equals(intent.getAction())) {
            // 每天凌晨1点填写“当天”的备注。
            fillToday(context);
            scheduleNextAlarm(context);
        }
    }

    public static void fillToday(Context context) {
        Calendar today = Calendar.getInstance();
        fillDate(context, today);
    }

    // 打开软件时立即补齐当月1号到今天的空白日期。
    // 只处理当前月份；已有备注或用户手动填写的备注不覆盖。
    public static void fillCurrentMonthMissing(Context context) {
        Calendar today = Calendar.getInstance();
        Calendar day = (Calendar) today.clone();
        day.set(Calendar.DAY_OF_MONTH, 1);

        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= todayDay; d++) {
            day.set(Calendar.DAY_OF_MONTH, d);
            fillDate(context, day);
        }
    }

    private static void fillDate(Context context, Calendar day) {
        int year = day.get(Calendar.YEAR);
        int month = day.get(Calendar.MONTH) + 1;
        int date = day.get(Calendar.DAY_OF_MONTH);

        String key = "kaoqin_note_" + year + "-" + String.format("%02d", month)
                + "-" + String.format("%02d", date);

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // 已有备注，或用户已经手动填写过，均不覆盖。
        if (prefs.contains(key) || prefs.getBoolean(MANUAL_PREFIX + key, false)) {
            return;
        }

        // 周六休息，其余日期正常。
        String value = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ? "休息" : "正常";

        prefs.edit()
                .putString(key, value)
                .putBoolean(MANUAL_PREFIX + key, false)
                .apply();
    }

    public static void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AttendanceAlarmReceiver.class);
        intent.setAction(ACTION_FILL);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 1);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.getTimeInMillis(),
                pendingIntent
        );
    }
}
