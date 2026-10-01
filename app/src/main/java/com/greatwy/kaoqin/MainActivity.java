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

public class MainActivity extends Activity {
    private WebView webView;
    private SharedPreferences notePrefs;
    private SharedPreferences peoplePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notePrefs = getSharedPreferences("kaoqin_notes", Context.MODE_PRIVATE);
        peoplePrefs = getSharedPreferences("kaoqin_multi_people", Context.MODE_PRIVATE);

        // 多人版每天凌晨1点为所有人员填写当天备注；错过闹钟时，打开APP后补填。
        AttendanceAlarmReceiver.scheduleNextAlarm(this);
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 1) {
            AttendanceAlarmReceiver.fillToday(this);
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);

        webView.addJavascriptInterface(new NoteStorage(), "AndroidStorage");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class NoteStorage {
        @JavascriptInterface
        public String getNote(String key) {
            return notePrefs.contains(key) ? notePrefs.getString(key, "") : null;
        }

        @JavascriptInterface
        public void saveNote(String key, String value) {
            notePrefs.edit()
                    .putString(key, value)
                    .putBoolean("manual_" + key, true)
                    .putBoolean("multi_manual_" + key.replace("multi_note_", ""), key.startsWith("multi_note_"))
                    .apply();
        }

        @JavascriptInterface
        public void removeNote(String key) {
            notePrefs.edit().remove(key).remove("manual_" + key)
                    .remove("multi_manual_" + key.replace("multi_note_", "")).apply();
        }

        @JavascriptInterface
        public String getPeople() {
            return peoplePrefs.getString("people", "[]");
        }

        @JavascriptInterface
        public void savePerson(String id, String name, String restDays) {
            try {
                JSONArray arr = new JSONArray(getPeople());
                JSONArray out = new JSONArray();
                boolean found = false;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.getJSONObject(i);
                    if (id.equals(p.optString("id"))) {
                        p.put("name", name);
                        p.put("restDays", restDays);
                        found = true;
                    }
                    out.put(p);
                }
                if (!found) {
                    JSONObject p = new JSONObject();
                    p.put("id", id);
                    p.put("name", name);
                    p.put("restDays", restDays);
                    out.put(p);
                }
                peoplePrefs.edit().putString("people", out.toString()).apply();
            } catch (Exception ignored) {
            }
        }

        @JavascriptInterface
        public void deletePerson(String id) {
            try {
                JSONArray arr = new JSONArray(getPeople());
                JSONArray out = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.getJSONObject(i);
                    if (!id.equals(p.optString("id"))) out.put(p);
                }
                peoplePrefs.edit().putString("people", out.toString()).apply();

                SharedPreferences.Editor e = notePrefs.edit();
                String prefix = "multi_note_" + id + "_";
                for (String key : notePrefs.getAll().keySet()) {
                    if (key.startsWith(prefix) || key.startsWith("multi_manual_" + id + "_")) e.remove(key);
                }
                e.apply();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
