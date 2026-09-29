package com.android.launcher3;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Incognito WebView whose downloads are encrypted directly into Void Vault. */
public class VoidPrivateBrowserActivity extends Activity {
    private static final int BG = Color.rgb(10, 10, 14);
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private WebView web;
    private EditText address;
    private ProgressBar progress;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL);
        Button back = new Button(this); back.setText("‹"); back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); }); bar.addView(back);
        address = new EditText(this); address.setSingleLine(true); address.setHint("Search or enter address"); address.setTextColor(Color.WHITE); address.setHintTextColor(Color.GRAY); address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setOnEditorActionListener((v, action, event) -> { if (action == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) { navigate(address.getText().toString()); return true; } return false; });
        bar.addView(address, new LinearLayout.LayoutParams(0, -1, 1));
        Button vault = new Button(this); vault.setText("Vault"); vault.setOnClickListener(v -> startActivity(new android.content.Intent(this, VoidVaultActivity.class))); bar.addView(vault);
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
        web = new WebView(this); root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
        WebSettings settings = web.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true); settings.setSaveFormData(false); settings.setAllowFileAccess(false); settings.setAllowContentAccess(false); settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient() { @Override public void onPageFinished(WebView view, String url) { address.setText(url); } });
        web.setWebChromeClient(new WebChromeClient() { @Override public void onProgressChanged(WebView view, int value) { progress.setProgress(value); progress.setVisibility(value == 100 ? android.view.View.GONE : android.view.View.VISIBLE); } });
        web.setDownloadListener((url, userAgent, disposition, mime, length) -> downloadToVault(url, userAgent, disposition, mime));
        navigate("https://www.google.com");
    }

    private void navigate(String input) {
        String value = input.trim();
        if (!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            if (value.contains(".") && !value.contains(" ")) value = "https://" + value;
            else value = "https://www.google.com/search?q=" + Uri.encode(value);
        }
        if (!value.startsWith("https://")) { Toast.makeText(this, "Only secure HTTPS pages are allowed", Toast.LENGTH_SHORT).show(); return; }
        web.loadUrl(value);
    }

    private void downloadToVault(String url, String userAgent, String disposition, String mime) {
        if (!url.startsWith("https://")) { Toast.makeText(this, "Blocked insecure download", Toast.LENGTH_SHORT).show(); return; }
        String name = URLUtil.guessFileName(url, disposition, mime);
        Toast.makeText(this, "Downloading privately…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setInstanceFollowRedirects(true); connection.setRequestProperty("User-Agent", userAgent);
                String cookies = CookieManager.getInstance().getCookie(url); if (cookies != null) connection.setRequestProperty("Cookie", cookies);
                connection.connect(); if (connection.getResponseCode() >= 400) throw new Exception("HTTP " + connection.getResponseCode());
                try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[16384]; int count; while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    new VoidVaultCrypto(this).store(out.toByteArray(), name, mime);
                }
                runOnUiThread(() -> Toast.makeText(this, "Encrypted in Void Vault", Toast.LENGTH_LONG).show());
            } catch (Exception error) { runOnUiThread(() -> Toast.makeText(this, "Private download failed: " + error.getMessage(), Toast.LENGTH_LONG).show()); }
            finally { if (connection != null) connection.disconnect(); }
        });
    }

    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.clearHistory(); web.clearCache(true); web.loadUrl("about:blank"); web.destroy(); }
        CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush();
        io.shutdownNow(); super.onDestroy();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
