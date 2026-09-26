package com.voidlauncher.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public final class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private LinearLayout content;
    private int wallpaperTarget;
    private static final int PICK_WALLPAPER = 41;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("void", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(15, 15, 18));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(28), dp(20), dp(48));
        scroll.addView(content);
        setContentView(scroll);

        TextView title = text("Void settings", 30, Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);
        content.addView(text("Pixel-style controls for each Void identity", 14, 0xffaaaab5));

        section("Appearance");
        toggle("Wallpaper background", "Show wallpaper behind the home screen", "wallpaper_enabled", true);
        toggle("Wallpaper dim", "Adds contrast behind icons and text", "wallpaper_dim", true);
        toggle("Motion parallax", "Wallpaper responds gently when the phone tilts", "parallax_enabled", true);
        toggle("Reduce motion", "Disables movement and longer animations", "reduce_motion", false);
        toggle("Transparent dock", "Use a translucent Pixel-style dock", "transparent_dock", true);
        toggle("Material You colours", "Derive launcher accents from the wallpaper", "material_you", true);
        button("Choose decoy wallpaper", v -> chooseWallpaper(0));
        button("Choose private wallpaper", v -> chooseWallpaper(1));
        button("Set lock-screen wallpaper", v -> chooseWallpaper(2));

        section("Unlock and privacy");
        button("Create or change private PIN", v -> changePrivatePin());
        button("Open Void Private Browser", v -> startActivity(new Intent(this, PrivateBrowserActivity.class)));
        toggle("Bottom decoy swipe", "Swipe up from the bottom to open the decoy app list", "bottom_drawer_swipe", true);
        toggle("Either-edge private swipe", "Swipe inward from the left or right edge for private unlock", "edge_private_swipe", true);
        toggle("Secure screenshots", "Blocks screenshots and launcher previews", "secure_window", true);
        toggle("Lock when leaving", "Returns to decoy whenever Void loses focus", "lock_on_leave", true);
        toggle("Clear private search", "Removes search text whenever Void locks", "clear_search", true);

        section("App drawer");
        toggle("Cache app list", "Show saved app information immediately", "cache_apps", true);
        toggle("Keep drawer ready", "Keeps the drawer mounted after first opening", "keep_drawer", true);
        toggle("Show app labels", "Display names below icons", "show_labels", true);
        toggle("Work Profile decoy", "Use isolated Work Profile apps and data as the bottom-swipe decoy", "work_profile_enabled", false);
        button("Create the Work Profile decoy", v -> createWorkProfile());

        section("Gestures");
        disabled("Swipe down for notifications", "Needs an approved Accessibility action on this phone");
        disabled("Double tap to lock", "Needs an approved Accessibility action on this phone");

        section("Coming after the v0.5 engine");
        disabled("Depth subject over clock", "Needs on-device subject segmentation");
        disabled("Face-follow wallpaper", "Needs a camera-based tracking engine and explicit camera permission");
        disabled("Folders and draggable icons", "Needs the new workspace database");
        disabled("Android widgets", "Needs AppWidget host support");
        disabled("Encrypted Void Vault", "Will encrypt private photos, documents and browser downloads with Android Keystore");

        section("System");
        button("Default launcher settings", v -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)));
        button("System wallpaper picker", v -> startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)));
    }

    private void chooseWallpaper(int target) {
        wallpaperTarget = target;
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(pick, PICK_WALLPAPER);
    }

    private void changePrivatePin() {
        VoidLock lock = new VoidLock(this);
        if (lock.isConfigured()) {
            EditText current = pinField("Current private PIN");
            AlertDialog verify = new AlertDialog.Builder(this).setTitle("Verify private PIN").setView(current)
                .setNegativeButton("Cancel", null).setPositiveButton("Continue", null).create();
            verify.setOnShowListener(ignored -> verify.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (!lock.authenticatePrivate(current.getText().toString())) { current.setError("Incorrect PIN"); return; }
                verify.dismiss(); showNewPrivatePin(lock);
            }));
            verify.show(); return;
        }
        showNewPrivatePin(lock);
    }

    private void showNewPrivatePin(VoidLock lock) {
        EditText pin = new EditText(this);
        pin.setHint("New 4 to 12 digit PIN"); pin.setSingleLine(true);
        pin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("Private swipe PIN").setView(pin).setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (dialog, which) -> {
                String value = pin.getText().toString();
                if (value.length() < 4 || value.length() > 12) { Toast.makeText(this, "PIN must have 4 to 12 digits", Toast.LENGTH_LONG).show(); return; }
                lock.configurePrivate(value); Toast.makeText(this, "Private PIN updated", Toast.LENGTH_SHORT).show();
            }).show();
    }

    private EditText pinField(String hint) {
        EditText field = new EditText(this); field.setHint(hint); field.setSingleLine(true);
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return field;
    }

    private void createWorkProfile() {
        if (!getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_MANAGED_USERS)) {
            Toast.makeText(this, "This phone does not support Android Work Profiles", Toast.LENGTH_LONG).show(); return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            new ComponentName(this, VoidAdminReceiver.class));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Work Profile setup is unavailable on this phone", Toast.LENGTH_LONG).show(); return;
        }
        prefs.edit().putBoolean("work_profile_enabled", true).apply();
        MainActivity.clearAppCaches();
        startActivity(intent);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_WALLPAPER || result != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        if (wallpaperTarget == 2) {
            try {
                WallpaperManager.getInstance(this).setStream(getContentResolver().openInputStream(uri), null, true, WallpaperManager.FLAG_LOCK);
                Toast.makeText(this, "Lock-screen wallpaper updated", Toast.LENGTH_SHORT).show();
            } catch (Exception error) { Toast.makeText(this, "This phone blocked lock-screen wallpaper changes", Toast.LENGTH_LONG).show(); }
        } else {
            prefs.edit().putString(wallpaperTarget == 0 ? "decoy_wallpaper" : "private_wallpaper", uri.toString()).apply();
            Toast.makeText(this, "Wallpaper saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void section(String name) {
        TextView view = text(name, 17, 0xffa78bfa);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(26), 0, dp(8)); content.addView(view);
    }
    private void toggle(String title, String summary, String key, boolean fallback) {
        Switch control = new Switch(this);
        control.setText(title + "\n" + summary); control.setTextColor(Color.WHITE); control.setTextSize(15);
        control.setPadding(0, dp(8), 0, dp(8)); control.setChecked(prefs.getBoolean(key, fallback));
        control.setOnCheckedChangeListener((v, checked) -> { prefs.edit().putBoolean(key, checked).apply(); MainActivity.clearAppCaches(); });
        content.addView(control, new LinearLayout.LayoutParams(-1, -2));
    }
    private void disabled(String title, String summary) {
        TextView view = text(title + "\n" + summary + "\nComing soon", 14, 0xff72727d);
        view.setPadding(0, dp(10), 0, dp(10)); content.addView(view);
    }
    private void button(String label, View.OnClickListener action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setOnClickListener(action);
        content.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }
    private TextView text(String value, int size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); return v; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
}
