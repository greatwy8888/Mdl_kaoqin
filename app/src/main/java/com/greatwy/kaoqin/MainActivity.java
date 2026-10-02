package com.greatwy.kaoqin;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;

public class MainActivity extends Activity {
    private WebView webView; private SharedPreferences notePrefs, peoplePrefs;
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        notePrefs=getSharedPreferences("kaoqin_notes",Context.MODE_PRIVATE);
        peoplePrefs=getSharedPreferences("kaoqin_multi_people",Context.MODE_PRIVATE);
        clearMultiNotesOnce();
        AttendanceAlarmReceiver.scheduleNextAlarm(this);
        if(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)>=1) AttendanceAlarmReceiver.fillToday(this);
        webView=new WebView(this); setContentView(webView);
        WebSettings s=webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setSupportZoom(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false); s.setTextZoom(100);
        webView.addJavascriptInterface(new NoteStorage(),"AndroidStorage"); webView.setWebViewClient(new WebViewClient()); webView.setWebChromeClient(new WebChromeClient()); webView.loadUrl("file:///android_asset/index.html");
    }
    private void clearMultiNotesOnce(){
        if(notePrefs.getBoolean("multi_notes_cleared_v2",false)) return;
        SharedPreferences.Editor e=notePrefs.edit();
        for(String k: notePrefs.getAll().keySet()){
            if(k.startsWith("multi_note_") || k.startsWith("manual_multi_note_")) e.remove(k);
        }
        e.putBoolean("multi_notes_cleared_v2",true).apply();
    }

    private class NoteStorage {
        @JavascriptInterface public String getNote(String key){return notePrefs.contains(key)?notePrefs.getString(key,""):null;}
        @JavascriptInterface public void saveNote(String key,String value){notePrefs.edit().putString(key,value).putBoolean("manual_"+key,true).apply();}
        @JavascriptInterface public void removeNote(String key){notePrefs.edit().remove(key).remove("manual_"+key).apply();}
        @JavascriptInterface public String getPeople(){return peoplePrefs.getString("people","[]");}
        @JavascriptInterface public boolean savePerson(String id,String name,String restWeekdays){
            try{JSONArray arr=new JSONArray(getPeople()),out=new JSONArray();boolean found=false;
                for(int i=0;i<arr.length();i++){JSONObject p=arr.getJSONObject(i);if(id.equals(p.optString("id"))){p.put("name",name);p.put("restWeekdays",restWeekdays);found=true;}out.put(p);}
                if(!found){JSONObject p=new JSONObject();p.put("id",id);p.put("name",name);p.put("restWeekdays",restWeekdays);out.put(p);}
                peoplePrefs.edit().putString("people",out.toString()).apply();return true;
            }catch(Exception e){return false;}
        }
        @JavascriptInterface public boolean deletePerson(String id){
            try{JSONArray arr=new JSONArray(getPeople()),out=new JSONArray();for(int i=0;i<arr.length();i++){JSONObject p=arr.getJSONObject(i);if(!id.equals(p.optString("id")))out.put(p);}
                peoplePrefs.edit().putString("people",out.toString()).apply();SharedPreferences.Editor e=notePrefs.edit();String prefix="multi_note_"+id+"_";Iterator<String> it=notePrefs.getAll().keySet().iterator();while(it.hasNext()){String k=it.next();if(k.startsWith(prefix)||k.startsWith("manual_"+prefix))e.remove(k);}e.apply();return true;
            }catch(Exception ex){return false;}
        }
    }
    @Override public void onBackPressed(){if(webView!=null&&webView.canGoBack())webView.goBack();else super.onBackPressed();}
}