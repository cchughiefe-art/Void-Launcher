package com.voidlauncher.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PrivateBrowserActivity extends AppCompatActivity {
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(2);
    private WebView web;
    private EditText address;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences("void", MODE_PRIVATE);
        PrivateAuth.authenticate(this, prefs, this::buildBrowser);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void buildBrowser() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xff09090b);
        LinearLayout bar = new LinearLayout(this); bar.setPadding(dp(8), dp(10), dp(8), dp(8));
        address = new EditText(this); address.setSingleLine(true); address.setHint("Search privately or enter address");
        address.setTextColor(Color.WHITE); address.setHintTextColor(0xff9999a4);
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button go = new Button(this); go.setText("Go"); bar.addView(go, new LinearLayout.LayoutParams(dp(68), dp(52)));
        Button gallery = new Button(this); gallery.setText("Vault");
        bar.addView(gallery, new LinearLayout.LayoutParams(dp(82), dp(52)));
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
        web.setDownloadListener(this::downloadPrivately);
        go.setOnClickListener(v -> navigate());
        gallery.setOnClickListener(v -> startActivity(new Intent(this, PrivateGalleryActivity.class)));
        address.setOnEditorActionListener((v, action, event) -> { navigate(); return true; });
    }

    private void downloadPrivately(String url, String userAgent, String disposition, String mime, long length) {
        String guessed = URLUtil.guessFileName(url, disposition, mime).replaceAll("[^a-zA-Z0-9._ -]", "_");
        Toast.makeText(this, "Downloading privately…", Toast.LENGTH_SHORT).show();
        DOWNLOADS.execute(() -> {
            File directory = new File(getFilesDir(), "private_downloads");
            if (!directory.exists() && !directory.mkdirs()) { showDownloadResult("Could not create private storage"); return; }
            File target = uniqueFile(directory, guessed);
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", userAgent == null ? "Void Private Browser" : userAgent);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) connection.setRequestProperty("Cookie", cookies);
                connection.setConnectTimeout(20_000); connection.setReadTimeout(60_000); connection.connect();
                if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300)
                    throw new java.io.IOException("HTTP " + connection.getResponseCode());
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(target)) {
                    byte[] buffer = new byte[32 * 1024]; int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
                showDownloadResult("Saved privately: " + target.getName());
            } catch (Exception error) {
                if (target.exists()) target.delete();
                showDownloadResult("Private download failed: " + error.getMessage());
            } finally { if (connection != null) connection.disconnect(); }
        });
    }

    private File uniqueFile(File directory, String name) {
        File file = new File(directory, name); int number = 1;
        int dot = name.lastIndexOf('.'); String base = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        while (file.exists()) file = new File(directory, base + " (" + number++ + ")" + extension);
        return file;
    }

    private void showDownloadResult(String message) { runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()); }

    private void navigate() {
        String value = address.getText().toString().trim();
        if (value.isEmpty()) return;
        if (!value.startsWith("http://") && !value.startsWith("https://"))
            value = "https://www.google.com/search?q=" + android.net.Uri.encode(value);
        web.loadUrl(value);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) { web.goBack(); return true; }
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
