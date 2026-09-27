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

public class MainActivity extends Activity {
    private WebView webView;
    private SharedPreferences notePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notePrefs = getSharedPreferences("kaoqin_notes", Context.MODE_PRIVATE);

        // 每天凌晨1点自动处理下一天；如果错过了闹钟，今天1点以后打开APP时补一次。
        AttendanceAlarmReceiver.scheduleNextAlarm(this);
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 1) {
            AttendanceAlarmReceiver.fillNextDay(this);
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
                    .apply();
        }

        @JavascriptInterface
        public void removeNote(String key) {
            notePrefs.edit()
                    .remove(key)
                    .remove("manual_" + key)
                    .apply();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
