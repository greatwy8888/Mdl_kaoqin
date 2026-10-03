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

        // 打开软件立即补齐当月1日至今天的空白日期；已有/手动备注不覆盖。
        AttendanceAlarmReceiver.fillCurrentMonthMissing(this);
        AttendanceAlarmReceiver.scheduleNextAlarm(this);

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
