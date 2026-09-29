package com.android.launcher3;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Secure, launcher-owned surface opened by an inward swipe from either edge. */
public class VoidPrivateSpaceActivity extends Activity {
    private static final int REQUEST_DEVICE_CREDENTIAL = 8421;
    private static final String PREFS = "void_private_space";
    private static final String KEY_BIOMETRIC = "biometric_lock";
    private static final String KEY_AUTO_LOCK = "auto_lock";
    private static final int BG = Color.rgb(10, 10, 14);
    private static final int ACCENT = Color.rgb(139, 92, 246);

    private CancellationSignal cancellationSignal;
    private SharedPreferences prefs;
    private boolean unlocked;
    private boolean authenticating;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow(); window.setStatusBarColor(BG); window.setNavigationBarColor(BG);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showLockedScreen();
    }

    @Override protected void onStart() {
        super.onStart();
        if (!unlocked && !authenticating) authenticate();
    }

    @Override protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && prefs.getBoolean(KEY_AUTO_LOCK, true)) {
            unlocked = false; authenticating = false;
            if (cancellationSignal != null) cancellationSignal.cancel();
        }
    }

    private void showLockedScreen() {
        LinearLayout root = baseLayout(); root.setGravity(Gravity.CENTER);
        TextView mark = title("VOID", 42); mark.setTextColor(ACCENT); root.addView(mark);
        root.addView(title("Private Space", 26));
        TextView hint = body("Swipe inward from either edge to enter. Your phone lock protects the encrypted vault and private browser.");
        hint.setGravity(Gravity.CENTER); root.addView(hint, margins(-1, -2, 32));
        Button unlock = button("Unlock Private Space"); unlock.setOnClickListener(v -> authenticate());
        root.addView(unlock, margins(-1, dp(54), 24)); setContentView(root);
    }

    private void authenticate() {
        if (!prefs.getBoolean(KEY_BIOMETRIC, true)) { showUnlocked(); return; }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) { requestDeviceCredential(); return; }
        authenticating = true;
        cancellationSignal = new CancellationSignal();
        BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                .setTitle("Unlock Void Private Space")
                .setSubtitle("Use your fingerprint or phone screen lock")
                .setNegativeButton("Use screen lock", getMainExecutor(), (dialog, which) -> requestDeviceCredential())
                .build();
        prompt.authenticate(cancellationSignal, getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) { authenticating = false; showUnlocked(); }
            @Override public void onAuthenticationError(int code, CharSequence message) {
                authenticating = false;
                if (code != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        && code != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                        && code != BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON) requestDeviceCredential();
            }
        });
    }

    private void requestDeviceCredential() {
        authenticating = false;
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isDeviceSecure()) {
            Intent intent = km.createConfirmDeviceCredentialIntent("Unlock Void Private Space", "Confirm your phone screen lock");
            if (intent != null) startActivityForResult(intent, REQUEST_DEVICE_CREDENTIAL);
        } else {
            Toast.makeText(this, "Set a phone screen lock to protect Private Space", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEVICE_CREDENTIAL && resultCode == RESULT_OK) showUnlocked();
    }

    private void showUnlocked() {
        unlocked = true; authenticating = false;
        LinearLayout root = baseLayout();
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(dp(22), dp(18), dp(22), dp(12));
        header.addView(title("Private Space", 30));
        header.addView(body("Encrypted vault · private downloads · protected by your phone lock"));
        root.addView(header);

        Switch biometric = toggle("Fingerprint / screen-lock gate", prefs.getBoolean(KEY_BIOMETRIC, true));
        biometric.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(KEY_BIOMETRIC, checked).apply()); root.addView(biometric);
        Switch autoLock = toggle("Lock whenever you leave", prefs.getBoolean(KEY_AUTO_LOCK, true));
        autoLock.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(KEY_AUTO_LOCK, checked).apply()); root.addView(autoLock);

        LinearLayout tools = new LinearLayout(this); tools.setOrientation(LinearLayout.HORIZONTAL);
        Button vault = button("Encrypted Vault"); vault.setOnClickListener(v -> startActivity(new Intent(this, VoidVaultActivity.class)));
        tools.addView(vault, new LinearLayout.LayoutParams(0, dp(54), 1));
        Button browser = button("Private Browser"); browser.setOnClickListener(v -> startActivity(new Intent(this, VoidPrivateBrowserActivity.class)));
        tools.addView(browser, new LinearLayout.LayoutParams(0, dp(54), 1)); root.addView(tools, margins(-1, dp(58), 10));

        Button systemPrivate = button("Set up Android Private Space");
        systemPrivate.setOnClickListener(v -> {
            Intent settings = new Intent("android.settings.PRIVATE_SPACE_SETTINGS");
            if (settings.resolveActivity(getPackageManager()) == null) {
                settings = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                Toast.makeText(this, "Your phone controls system Private Space from Security settings", Toast.LENGTH_LONG).show();
            }
            startActivity(settings);
        });
        root.addView(systemPrivate, margins(-1, dp(54), 8));

        TextView appsHeading = body("PRIVATE APP DRAWER"); appsHeading.setTextColor(ACCENT); appsHeading.setPadding(dp(22), dp(22), dp(22), dp(8)); root.addView(appsHeading);
        List<ResolveInfo> apps = loadLaunchableApps(); List<String> labels = new ArrayList<>(); PackageManager pm = getPackageManager();
        for (ResolveInfo info : apps) labels.add(info.loadLabel(pm).toString());
        ListView list = new ListView(this); list.setDividerHeight(0);
        list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout row = new LinearLayout(VoidPrivateSpaceActivity.this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(22), dp(10), dp(22), dp(10));
                ImageView icon = new ImageView(VoidPrivateSpaceActivity.this); icon.setImageDrawable(apps.get(position).loadIcon(pm)); row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
                TextView text = title(getItem(position), 17); text.setPadding(dp(18), 0, 0, 0); row.addView(text, new LinearLayout.LayoutParams(0, dp(58), 1)); return row;
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            ResolveInfo info = apps.get(position); Intent launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName); if (launch != null) startActivity(launch);
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
    }

    private List<ResolveInfo> loadLaunchableApps() {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = new ArrayList<>(getPackageManager().queryIntentActivities(query, 0));
        apps.removeIf(info -> getPackageName().equals(info.activityInfo.packageName));
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(getPackageManager())); return apps;
    }

    private LinearLayout baseLayout() { LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setPadding(dp(16), dp(16), dp(16), dp(16)); return root; }
    private TextView title(String value, int sp) { TextView view = new TextView(this); view.setText(value); view.setTextColor(Color.WHITE); view.setTextSize(sp); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private TextView body(String value) { TextView view = title(value, 15); view.setTextColor(Color.rgb(190, 186, 201)); view.setPadding(0, dp(8), 0, dp(8)); return view; }
    private Switch toggle(String value, boolean checked) { Switch view = new Switch(this); view.setText(value); view.setTextColor(Color.WHITE); view.setTextSize(16); view.setChecked(checked); view.setPadding(dp(22), dp(12), dp(12), dp(12)); return view; }
    private Button button(String value) { Button view = new Button(this); view.setText(value); view.setTextColor(Color.WHITE); GradientDrawable bg = new GradientDrawable(); bg.setColor(ACCENT); bg.setCornerRadius(dp(28)); view.setBackground(bg); return view; }
    private LinearLayout.LayoutParams margins(int width, int height, int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height); p.setMargins(dp(16), dp(top), dp(16), 0); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
