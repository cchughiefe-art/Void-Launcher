package com.voidlauncher.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public final class PrivateBrowserActivity extends AppCompatActivity {
    private WebView web;
    private EditText address;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xff09090b);
        LinearLayout bar = new LinearLayout(this); bar.setPadding(dp(8), dp(10), dp(8), dp(8));
        address = new EditText(this); address.setSingleLine(true); address.setHint("Search privately or enter address");
        address.setTextColor(Color.WHITE); address.setHintTextColor(0xff9999a4);
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button go = new Button(this); go.setText("Go"); bar.addView(go, new LinearLayout.LayoutParams(dp(68), dp(52)));
        root.addView(bar);
        web = new WebView(this); root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(false);
        web.getSettings().setDatabaseEnabled(false);
        web.getSettings().setGeolocationEnabled(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (android.os.Build.VERSION.SDK_INT >= 26) web.getSettings().setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient());
        web.setDownloadListener((url, userAgent, disposition, mime, length) ->
            Toast.makeText(this, "Downloads stay blocked until Void Vault encryption is enabled", Toast.LENGTH_LONG).show());
        go.setOnClickListener(v -> navigate());
        address.setOnEditorActionListener((v, action, event) -> { navigate(); return true; });
    }

    private void navigate() {
        String value = address.getText().toString().trim();
        if (value.isEmpty()) return;
        if (!value.startsWith("http://") && !value.startsWith("https://"))
            value = "https://www.google.com/search?q=" + android.net.Uri.encode(value);
        web.loadUrl(value);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) finishAndRemoveTask();
    }

    @Override protected void onDestroy() {
        if (web != null) {
            web.stopLoading(); web.clearHistory(); web.clearCache(true); web.clearFormData();
            web.loadUrl("about:blank"); web.removeAllViews(); web.destroy();
        }
        CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        super.onDestroy();
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
}
