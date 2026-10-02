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
 private static final String PREFS="kaoqin_notes", PEOPLE_PREFS="kaoqin_multi_people", ACTION_FILL="com.greatwy.kaoqin.FILL_NEXT_DAY";
 @Override public void onReceive(Context c,Intent i){if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){scheduleNextAlarm(c);return;}if(ACTION_FILL.equals(i.getAction())){fillToday(c);scheduleNextAlarm(c);}}
 public static void fillToday(Context c){
  Calendar t=Calendar.getInstance();int y=t.get(Calendar.YEAR),m=t.get(Calendar.MONTH)+1,d=t.get(Calendar.DAY_OF_MONTH),w=((t.get(Calendar.DAY_OF_WEEK)+5)%7)+1;
  String date=y+"-"+String.format("%02d",m)+"-"+String.format("%02d",d);SharedPreferences n=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE),p=c.getSharedPreferences(PEOPLE_PREFS,Context.MODE_PRIVATE);
  try{JSONArray arr=new JSONArray(p.getString("people","[]"));SharedPreferences.Editor e=n.edit();
   for(int i=0;i<arr.length();i++){JSONObject x=arr.getJSONObject(i);String id=x.optString("id");if(id.isEmpty())continue;String key="multi_note_"+id+"_"+date;if(n.getBoolean("manual_"+key,false))continue;
    boolean rest=false; String raw=x.optString("restWeekdays","[]"); try{ JSONArray ws=new JSONArray(raw); for(int j=0;j<ws.length();j++) if(w==ws.optInt(j)) {rest=true;break;} }catch(Exception ignored){ }e.putString(key,rest?"休息":"正常").putBoolean("manual_"+key,false);}
   e.apply();
  }catch(Exception ignored){}
 }
 public static void scheduleNextAlarm(Context c){AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,AttendanceAlarmReceiver.class);i.setAction(ACTION_FILL);PendingIntent p=PendingIntent.getBroadcast(c,1001,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Calendar n=Calendar.getInstance();n.set(Calendar.HOUR_OF_DAY,1);n.set(Calendar.MINUTE,0);n.set(Calendar.SECOND,0);n.set(Calendar.MILLISECOND,0);if(!n.after(Calendar.getInstance()))n.add(Calendar.DAY_OF_MONTH,1);a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.getTimeInMillis(),p);}
}