package com.greatwy.kaoqin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

public class AttendanceAlarmReceiver extends BroadcastReceiver {
    private static final String PREFS="kaoqin_notes", PEOPLE_PREFS="kaoqin_multi_people", ACTION_FILL="com.greatwy.kaoqin.FILL_NEXT_DAY";
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){scheduleNextAlarm(context);return;}
        if(ACTION_FILL.equals(intent.getAction())){fillToday(context);scheduleNextAlarm(context);}
    }
    public static void fillToday(Context context){
        Calendar today=Calendar.getInstance(); int y=today.get(Calendar.YEAR),m=today.get(Calendar.MONTH)+1,d=today.get(Calendar.DAY_OF_MONTH);
        String date=y+"-"+String.format("%02d",m)+"-"+String.format("%02d",d);
        SharedPreferences notes=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE), people=context.getSharedPreferences(PEOPLE_PREFS,Context.MODE_PRIVATE);
        try{
            JSONArray arr=new JSONArray(people.getString("people","[]")); SharedPreferences.Editor e=notes.edit();
            for(int i=0;i<arr.length();i++){
                JSONObject p=arr.getJSONObject(i); String id=p.optString("id"); if(id.isEmpty())continue;
                String key="multi_note_"+id+"_"+date;
                if(notes.getBoolean("manual_"+key,false)||notes.contains(key))continue;
                boolean rest=false; JSONArray dates=new JSONArray(p.optString("restDates","[]"));
                for(int j=0;j<dates.length();j++)if(date.equals(dates.optString(j))){rest=true;break;}
                e.putString(key,rest?"休息":"正常");
            }
            e.apply();
        }catch(Exception ignored){}
    }
    public static void scheduleNextAlarm(Context context){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE); Intent i=new Intent(context,AttendanceAlarmReceiver.class);i.setAction(ACTION_FILL);
        PendingIntent pi=PendingIntent.getBroadcast(context,1001,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,1);next.set(Calendar.MINUTE,0);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
        if(!next.after(Calendar.getInstance()))next.add(Calendar.DAY_OF_MONTH,1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
    }
}