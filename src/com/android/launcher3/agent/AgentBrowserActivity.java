package com.android.launcher3.agent;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Visible browser, login and connection controls. Credentials never enter the command protocol. */
public class AgentBrowserActivity extends Activity {
    private final Handler main = new Handler();
    private AgentSettings settings;
    private AgentBrowserService service;
    private LinearLayout browserHost;
    private EditText address;
    private TextView status;
    private boolean bound, resumed;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            status.setText(settings.prefs.getString("status", "Not connected") + (settings.enabled() ? "\nConnection enabled until Stop" : "\nConnection off"));
            main.postDelayed(this, 1000);
        }
    };
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            service = ((AgentBrowserService.LocalBinder)b).service(); attachBrowser();
        }
        @Override public void onServiceDisconnected(ComponentName n) { service = null; status.setText("Browser process disconnected; reopen this screen to recover."); }
    };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if (Build.VERSION.SDK_INT < 28) { new AlertDialog.Builder(this).setMessage("Agent Browser requires Android 9 or newer for a separate browser profile.").setPositiveButton("Close", (d,w) -> finish()).setOnDismissListener(d -> finish()).show(); return; } settings = new AgentSettings(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(Color.rgb(10,10,14)); getWindow().setNavigationBarColor(Color.rgb(10,10,14));
        LinearLayout root = column(); root.setPadding(dp(12), dp(10), dp(12), 0);
        TextView title = label("VOID Agent Browser", 22); root.addView(title);
        status = label("Opening browser…", 13); root.addView(status);
        LinearLayout controls = new LinearLayout(this);
        addButton(controls, "Start", () -> startConnection());
        addButton(controls, "Stop", () -> { if (service != null) service.stopConnection(); else { settings.enabled(false); settings.status("Stopped by you"); } });
        addButton(controls, "Settings", this::showSettings); root.addView(controls);
        LinearLayout bar = new LinearLayout(this);
        address = new EditText(this); address.setSingleLine(); address.setTextColor(Color.WHITE); address.setHintTextColor(Color.GRAY); address.setHint("https://x.com"); address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        bar.addView(address, new LinearLayout.LayoutParams(0, dp(48), 1)); addButton(bar, "Go", this::navigate); root.addView(bar);
        browserHost = column(); root.addView(browserHost, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
    }
    @Override protected void onStart() {
        super.onStart(); if (settings == null) return; bound = bindService(new Intent(this, AgentBrowserService.class), connection, BIND_AUTO_CREATE);
    }
    @Override protected void onResume() { super.onResume(); if (settings == null) return; resumed = true; main.post(refresh); }
    @Override protected void onPause() { resumed = false; main.removeCallbacks(refresh); super.onPause(); }
    @Override protected void onStop() {
        if (service != null) service.engine().detach(getApplicationContext());
        if (bound) { unbindService(connection); bound = false; } service = null; super.onStop();
    }
    private void attachBrowser() {
        if (service == null || service.engine() == null) { status.setText("WebView unavailable."); return; }
        WebView web = service.engine().view(this); browserHost.removeAllViews(); browserHost.addView(web, new LinearLayout.LayoutParams(-1, -1));
        String current = web.getUrl();
        if (current == null || current.equals("about:blank")) {
            String last = settings.prefs.getString("last_url", "https://x.com");
            try { service.engine().navigateLocal(last); } catch (Exception ignored) {}
        }
        address.setText(current != null && current.startsWith("https://") ? current : "https://x.com");
    }
    private void navigate() {
        if (service == null) return;
        try { service.engine().navigateLocal(address.getText().toString().trim()); }
        catch (Exception e) { status.setText(e.getMessage()); }
    }
    private void startConnection() {
        try {
            if (settings.deviceKey().isEmpty()) { showSettings(); return; }
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 8042); return;
            }
            settings.enabled(true); settings.status("Connecting…");
            startForegroundService(new Intent(this, AgentBrowserService.class));
        } catch (Exception e) { settings.enabled(false); status.setText("Could not start. Check saved device key and Android service permissions."); }
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 8042 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startConnection();
        else if (request == 8042) status.setText("Allow notifications so the running connection and Stop control remain visible.");
    }
    private void showSettings() {
        LinearLayout content = column(); content.setPadding(dp(20), dp(8), dp(20), 0);
        TextView note = label("Enter your existing device key once. Relay discovery is automatic. Pages you ask the agent to read go through your server into your private void-x-bot repository. Sign in to X in this browser yourself. No form submission or publishing through the bridge.", 14); content.addView(note);
        EditText key = new EditText(this); key.setTextColor(Color.WHITE); key.setHintTextColor(Color.GRAY); key.setHint("Device key (leave blank to keep saved)"); key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); key.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO); content.addView(key);
        CheckBox boot = new CheckBox(this); boot.setText("Resume enabled connection after restart"); boot.setTextColor(Color.WHITE); boot.setChecked(settings.prefs.getBoolean("resume_boot", true)); content.addView(boot);
        addButton(content, "Android battery settings", () -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + getPackageName()))));
        new AlertDialog.Builder(this).setTitle("Agent Browser settings").setView(content).setNegativeButton("Cancel", null)
                .setNeutralButton("Forget device key", (d, w) -> { if (service != null) service.stopConnection(); settings.enabled(false); settings.prefs.edit().remove("key_enc").remove("key_iv").apply(); settings.status("Device key removed. Browser login is unchanged."); })
                .setPositiveButton("Save", (d, w) -> {
                    try { if (!key.getText().toString().trim().isEmpty()) settings.saveDeviceKey(key.getText().toString().trim()); settings.prefs.edit().putBoolean("resume_boot", boot.isChecked()).apply(); settings.status("Settings saved. Tap Start to connect."); }
                    catch (Exception e) { status.setText("Could not save device key. Check its length and try again."); }
                }).show();
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setBackgroundColor(Color.rgb(10,10,14)); return l; }
    private TextView label(String text, int size) { TextView t = new TextView(this); t.setText(text); t.setTextColor(Color.WHITE); t.setTextSize(size); t.setPadding(0, dp(4), 0, dp(4)); return t; }
    private void addButton(LinearLayout parent, String text, Runnable action) { Button b = new Button(this); b.setText(text); b.setOnClickListener(v -> action.run()); parent.addView(b); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
