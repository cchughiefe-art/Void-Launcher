package com.voidlauncher.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
        getWindow().setStatusBarColor(0xff0f0f12);
        getWindow().setNavigationBarColor(0xff0f0f12);
        prefs = getSharedPreferences("void", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(15, 15, 18));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(54), dp(18), dp(64));
        scroll.addView(content);
        setContentView(scroll);

        TextView title = text("Void settings", 30, Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);
        content.addView(text("Pixel-style controls for Main and Private Space", 14, 0xffaaaab5));

        section("Appearance");
        toggle("Wallpaper background", "Show wallpaper behind the home screen", "wallpaper_enabled", true);
        toggle("Wallpaper dim", "Adds contrast behind icons and text", "wallpaper_dim", true);
        toggle("Motion parallax", "Wallpaper responds gently when the phone tilts", "parallax_enabled", true);
        toggle("Reduce motion", "Disables movement and longer animations", "reduce_motion", false);
        toggle("Transparent dock", "Use a translucent Pixel-style dock", "transparent_dock", true);
        button("Choose Private Space wallpaper", v -> chooseWallpaper(0));
        button("Choose main wallpaper", v -> chooseWallpaper(1));
        button("Set lock-screen wallpaper", v -> chooseWallpaper(2));

        section("Built-in wallpapers");
        button("Void Purple", v -> choosePreset("builtin:void", R.drawable.wallpaper_void));
        button("Pixel Blue", v -> choosePreset("builtin:blue", R.drawable.wallpaper_pixel_blue));
        button("Sunrise", v -> choosePreset("builtin:sunrise", R.drawable.wallpaper_sunrise));
        button("Emerald", v -> choosePreset("builtin:emerald", R.drawable.wallpaper_emerald));

        section("Wallpaper layout");
        button("Focus: top", v -> setString("wallpaper_focus", "top", "Wallpaper focus set to top"));
        button("Focus: center", v -> setString("wallpaper_focus", "center", "Wallpaper focus set to center"));
        button("Focus: bottom", v -> setString("wallpaper_focus", "bottom", "Wallpaper focus set to bottom"));
        button("Zoom: subtle", v -> setInt("wallpaper_zoom", 106, "Subtle wallpaper zoom"));
        button("Zoom: balanced", v -> setInt("wallpaper_zoom", 110, "Balanced wallpaper zoom"));
        button("Zoom: deep", v -> setInt("wallpaper_zoom", 120, "Deep wallpaper zoom"));
        button("Dim: light", v -> setInt("wallpaper_dim_strength", 22, "Light wallpaper dim"));
        button("Dim: balanced", v -> setInt("wallpaper_dim_strength", 40, "Balanced wallpaper dim"));
        button("Dim: dark", v -> setInt("wallpaper_dim_strength", 62, "Dark wallpaper dim"));
        toggle("Wallpaper blur", "Soft blur on Android 12 and newer", "wallpaper_blur", false);
        toggle("Depth subject over clock", "Place a detected person, pet or object in front of the home clock", "depth_effect", true);
        button("Rebuild Main depth layer", v -> rebuildDepth("main"));
        button("Rebuild Private depth layer", v -> rebuildDepth("private"));
        button("Motion: gentle", v -> setInt("parallax_strength", 10, "Gentle wallpaper motion"));
        button("Motion: balanced", v -> setInt("parallax_strength", 18, "Balanced wallpaper motion"));
        button("Motion: strong", v -> setInt("parallax_strength", 28, "Strong wallpaper motion"));

        section("Unlock and privacy");
        button("Open Void Private Browser", v -> startActivity(new Intent(this, PrivateBrowserActivity.class)));
        button("Open Private Gallery", v -> startActivity(new Intent(this, PrivateGalleryActivity.class)));
        toggle("Fingerprint Private Space lock", "Require an enrolled fingerprint before entry", "biometric_private_lock", true);
        toggle("Bottom main swipe", "Swipe up from the bottom to open the full main app list", "bottom_drawer_swipe", true);
        toggle("Either-edge Private Space", "Swipe inward from the left or right edge to open Private Space", "edge_private_swipe", true);
        toggle("Close Private Space when leaving", "Returns to the main space whenever Void loses focus", "lock_on_leave", true);
        toggle("Clear app search", "Removes search text whenever Void closes", "clear_search", true);

        section("App drawer");
        toggle("Cache app list", "Show saved app information immediately", "cache_apps", true);
        toggle("Keep drawer ready", "Keeps the drawer mounted after first opening", "keep_drawer", true);
        toggle("Show app labels", "Display names below icons", "show_labels", true);
        toggle("Reverse alphabetical order", "Sort the drawer from Z to A instead of A to Z", "sort_descending", false);
        button("Use 4-column grid", v -> setColumns(4));
        button("Use 5-column grid", v -> setColumns(5));
        button("Clear Home screen apps", v -> clearHomeApps());
        button("Reset Main dock", v -> clearSet("main_favorites", "Main dock reset"));
        button("Unhide all Main apps", v -> clearSet("main_hidden", "All Main apps are visible"));

        section("Private Space");
        button("Clear Private Space app list", v -> clearPrivateApps());
        button("Reset Private dock", v -> clearSet("private_favorites", "Private dock reset"));
        button("Unhide all Private apps", v -> clearSet("private_hidden", "All Private apps are visible"));
        button("Reset Private Space", v -> resetPrivateSpace());

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

    private void choosePreset(String preset, int drawable) {
        new AlertDialog.Builder(this).setTitle("Apply wallpaper to")
            .setItems(new String[]{"Main home", "Private Space", "Lock screen"}, (dialog, which) -> {
                if (which == 2) {
                    try {
                        WallpaperManager.getInstance(this).setResource(drawable, WallpaperManager.FLAG_LOCK);
                        Toast.makeText(this, "Lock-screen wallpaper updated", Toast.LENGTH_SHORT).show();
                    } catch (Exception error) { Toast.makeText(this, "This phone blocked the lock-screen change", Toast.LENGTH_LONG).show(); }
                } else {
                    String space = which == 0 ? "main" : "private";
                    clearDepth(space);
                    prefs.edit().putString(space + "_wallpaper", preset).apply();
                    Toast.makeText(this, which == 0 ? "Main wallpaper updated" : "Private wallpaper updated", Toast.LENGTH_SHORT).show();
                }
            }).show();
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
            String space = wallpaperTarget == 0 ? "private" : "main";
            clearDepth(space);
            prefs.edit().putString(space + "_wallpaper", uri.toString()).apply();
            Toast.makeText(this, "Wallpaper saved", Toast.LENGTH_SHORT).show();
            generateDepth(space, uri.toString());
        }
    }

    private void rebuildDepth(String space) {
        String wallpaper = prefs.getString(space + "_wallpaper", "");
        if (wallpaper.isEmpty()) wallpaper = "builtin:void";
        clearDepth(space); generateDepth(space, wallpaper);
    }

    private void generateDepth(String space, String wallpaperValue) {
        Toast.makeText(this, "Generating " + space + " depth layer…", Toast.LENGTH_LONG).show();
        WallpaperDepthEngine.Callback callback = (success, message) -> runOnUiThread(() ->
            Toast.makeText(this, message, success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
        int resource = presetDrawable(wallpaperValue);
        if (resource != 0) WallpaperDepthEngine.generate(this, resource, space, callback);
        else WallpaperDepthEngine.generate(this, Uri.parse(wallpaperValue), space, callback);
    }

    private int presetDrawable(String value) {
        if ("builtin:void".equals(value)) return R.drawable.wallpaper_void;
        if ("builtin:blue".equals(value)) return R.drawable.wallpaper_pixel_blue;
        if ("builtin:sunrise".equals(value)) return R.drawable.wallpaper_sunrise;
        if ("builtin:emerald".equals(value)) return R.drawable.wallpaper_emerald;
        return 0;
    }

    private void clearDepth(String space) {
        String old = prefs.getString("depth_" + space + "_path", "");
        if (!old.isEmpty()) new java.io.File(old).delete();
        prefs.edit().remove("depth_" + space + "_path").apply();
    }

    private void section(String name) {
        TextView view = text(name, 17, 0xffa78bfa);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(26), 0, dp(8)); content.addView(view);
    }
    private void toggle(String title, String summary, String key, boolean fallback) {
        Switch control = new Switch(this);
        control.setText(title + "\n" + summary); control.setTextColor(Color.WHITE); control.setTextSize(15);
        control.setPadding(dp(14), dp(12), dp(12), dp(12)); control.setMinHeight(dp(72));
        GradientDrawable background = new GradientDrawable(); background.setColor(0xff17151c); background.setCornerRadius(dp(18));
        control.setBackground(background); control.setChecked(prefs.getBoolean(key, fallback));
        control.setOnCheckedChangeListener((v, checked) -> { prefs.edit().putBoolean(key, checked).apply(); MainActivity.clearAppCaches(); });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, dp(4), 0, dp(4));
        content.addView(control, params);
    }
    private void button(String label, View.OnClickListener action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextColor(Color.WHITE);
        button.setTextSize(15); button.setMinHeight(dp(56)); button.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable card = new GradientDrawable(); card.setColor(0xff24212b); card.setCornerRadius(dp(18));
        card.setStroke(dp(1), 0xff3a3544);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x337c4dff), card, null));
        button.setOnClickListener(action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58)); params.setMargins(0, dp(4), 0, dp(4));
        content.addView(button, params);
    }
    private void setColumns(int columns) {
        prefs.edit().putInt("columns", columns).apply();
        Toast.makeText(this, columns + " columns selected; reopen Void to apply", Toast.LENGTH_SHORT).show();
    }
    private void clearPrivateApps() {
        prefs.edit().putStringSet("private_allowed", new java.util.HashSet<>()).putBoolean("private_initialized", true).apply();
        MainActivity.clearAppCaches();
        Toast.makeText(this, "Private Space app list cleared", Toast.LENGTH_SHORT).show();
    }
    private void clearHomeApps() {
        prefs.edit().remove("main_home").apply(); MainActivity.clearAppCaches();
        Toast.makeText(this, "Home screen apps cleared", Toast.LENGTH_SHORT).show();
    }
    private void clearSet(String key, String message) {
        prefs.edit().remove(key).apply(); MainActivity.clearAppCaches();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void resetPrivateSpace() {
        clearDepth("private");
        prefs.edit().remove("private_allowed").remove("private_initialized").remove("private_favorites")
            .remove("private_hidden").remove("private_wallpaper").apply();
        MainActivity.clearAppCaches();
        Toast.makeText(this, "Private Space reset", Toast.LENGTH_SHORT).show();
    }
    private void setInt(String key, int value, String message) {
        prefs.edit().putInt(key, value).apply(); Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void setString(String key, String value, String message) {
        prefs.edit().putString(key, value).apply(); Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private TextView text(String value, int size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); return v; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
}
