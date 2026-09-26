package com.voidlauncher.app;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends AppCompatActivity implements AppAdapter.Listener {
    private final List<AppEntry> allApps = new ArrayList<>();
    private AppAdapter adapter;
    private View drawer;
    private EditText search;
    private LinearLayout dock;
    private View settingsButton;
    private SharedPreferences prefs;
    private VoidLock voidLock;
    private VoidLock.Profile activeProfile;
    private float touchDownY;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        prefs = getSharedPreferences("void", MODE_PRIVATE);
        voidLock = new VoidLock(this);
        drawer = findViewById(R.id.drawer);
        search = findViewById(R.id.search);
        dock = findViewById(R.id.dock);
        settingsButton = findViewById(R.id.settings);
        RecyclerView grid = findViewById(R.id.apps);
        grid.setLayoutManager(new GridLayoutManager(this, prefs.getInt("columns", 4)));
        adapter = new AppAdapter(this);
        grid.setAdapter(adapter);

        findViewById(R.id.openDrawer).setOnClickListener(v -> requestDrawerUnlock());
        settingsButton.setOnClickListener(v -> showSettings());
        findViewById(R.id.homeScreen).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) touchDownY = event.getY();
            if (event.getAction() == MotionEvent.ACTION_UP && touchDownY - event.getY() > 110) {
                requestDrawerUnlock(); return true;
            }
            return true;
        });
        search.addTextChangedListener(new SimpleTextWatcher(this::filter));
        search.setOnEditorActionListener((v, action, event) -> {
            AppEntry first = adapter.first(); if (first != null) open(first); return true;
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if (drawer.getVisibility() == View.VISIBLE) hideDrawer(); }
        });
        loadApps();
        if (!prefs.getBoolean("default_prompted", false)) {
            prefs.edit().putBoolean("default_prompted", true).apply();
            findViewById(R.id.root).postDelayed(this::requestDefaultLauncher, 700);
        }
    }

    @Override protected void onResume() { super.onResume(); if (adapter != null) loadApps(); }

    @Override protected void onStop() {
        super.onStop();
        activeProfile = null;
        if (search != null) search.setText("");
        if (drawer != null) drawer.setVisibility(View.GONE);
    }

    private void loadApps() {
        LauncherApps service = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        String profile = activeProfile == VoidLock.Profile.PRIVATE ? "private_" : "decoy_";
        Set<String> hidden = prefs.getStringSet(profile + "hidden", Collections.emptySet());
        boolean privateProfile = activeProfile == VoidLock.Profile.PRIVATE;
        List<LauncherActivityInfo> infos = new ArrayList<>();
        UserManager users = (UserManager) getSystemService(USER_SERVICE);
        for (UserHandle user : users.getUserProfiles()) {
            boolean work = !user.equals(Process.myUserHandle());
            if (work && (!privateProfile || !prefs.getBoolean("work_profile_enabled", false))) continue;
            try { infos.addAll(service.getActivityList(null, user)); } catch (SecurityException ignored) {}
        }
        if (!privateProfile && !prefs.getBoolean("decoy_initialized", false)) {
            Set<String> starter = new HashSet<>();
            for (LauncherActivityInfo info : infos) if (safeDecoyDefault(info)) starter.add(info.getComponentName().flattenToString());
            prefs.edit().putStringSet("decoy_allowed", starter).putBoolean("decoy_initialized", true).apply();
        }
        Set<String> decoyAllowed = prefs.getStringSet("decoy_allowed", Collections.emptySet());
        allApps.clear();
        for (LauncherActivityInfo info : infos) {
            if (info.getComponentName().getPackageName().equals(getPackageName())) continue;
            if (hidden.contains(info.getComponentName().flattenToString())) continue;
            if (!privateProfile && !decoyAllowed.contains(info.getComponentName().flattenToString())) continue;
            boolean work = !info.getUser().equals(Process.myUserHandle());
            String label = info.getLabel().toString() + (work ? " · Work" : "");
            allApps.add(new AppEntry(label, info.getComponentName(), info.getBadgedIcon(0), info.getUser(), work));
        }
        allApps.sort((a,b) -> a.label.compareToIgnoreCase(b.label));
        adapter.submit(allApps);
        settingsButton.setVisibility(privateProfile ? View.VISIBLE : View.GONE);
        buildDock();
    }

    private boolean safeDecoyDefault(LauncherActivityInfo info) {
        String value = (info.getLabel() + " " + info.getComponentName().getPackageName()).toLowerCase(Locale.ROOT);
        String[] safe = {"phone", "dialer", "message", "camera", "calculator", "clock", "calendar", "files", "chrome"};
        for (String word : safe) if (value.contains(word)) return true;
        return false;
    }

    private void filter(String value) {
        String q = value.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) { adapter.submit(allApps); return; }
        List<AppEntry> result = new ArrayList<>();
        for (AppEntry app : allApps) if (app.label.toLowerCase(Locale.ROOT).contains(q)) result.add(app);
        adapter.submit(result);
    }

    private void buildDock() {
        dock.removeAllViews();
        String profile = activeProfile == VoidLock.Profile.PRIVATE ? "private_" : "decoy_";
        Set<String> favorites = prefs.getStringSet(profile + "favorites", Collections.emptySet());
        List<AppEntry> chosen = new ArrayList<>();
        for (AppEntry app : allApps) if (favorites.contains(app.component.flattenToString())) chosen.add(app);
        if (chosen.isEmpty()) {
            String[] common = {"phone", "message", "chrome", "camera"};
            for (String needle : common) for (AppEntry app : allApps) {
                String text = (app.label + app.component.getPackageName()).toLowerCase(Locale.ROOT);
                if (text.contains(needle) && !chosen.contains(app)) { chosen.add(app); break; }
            }
        }
        for (AppEntry app : chosen.subList(0, Math.min(5, chosen.size()))) {
            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            icon.setLayoutParams(lp); icon.setPadding(12, 6, 12, 6); icon.setImageDrawable(app.icon);
            icon.setContentDescription(app.label); icon.setOnClickListener(v -> open(app));
            icon.setOnLongClickListener(v -> { menu(app, v); return true; }); dock.addView(icon);
        }
    }

    private void requestDrawerUnlock() {
        if (!voidLock.isConfigured()) { showLockSetup(); return; }
        final EditText pin = pinField("Enter Void PIN");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Void Lock").setView(pin)
            .setNegativeButton("Cancel", null).setPositiveButton("Unlock", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            VoidLock.Profile result = voidLock.authenticate(pin.getText().toString());
            if (result == null) { pin.setError("Incorrect PIN"); return; }
            activeProfile = result; loadApps(); dialog.dismiss(); showDrawer();
        }));
        dialog.show();
    }

    private EditText pinField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        field.setSingleLine(true);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        field.setPadding(pad, pad / 2, pad, pad / 2);
        return field;
    }

    private void showLockSetup() {
        final EditText privatePin = pinField("Create private PIN");
        new AlertDialog.Builder(this).setTitle("Create Void Lock").setMessage("Use 4 to 12 digits.")
            .setView(privatePin).setNegativeButton("Cancel", null).setPositiveButton("Next", (d, w) -> {
                String first = privatePin.getText().toString();
                if (first.length() < 4 || first.length() > 12) { Toast.makeText(this, "Private PIN must have 4 to 12 digits", Toast.LENGTH_LONG).show(); return; }
                final EditText decoyPin = pinField("Create different decoy PIN");
                new AlertDialog.Builder(this).setTitle("Create decoy unlock").setView(decoyPin)
                    .setNegativeButton("Cancel", null).setPositiveButton("Save", (d2, w2) -> {
                        String second = decoyPin.getText().toString();
                        if (second.length() < 4 || second.length() > 12 || first.equals(second)) {
                            Toast.makeText(this, "Use a different 4 to 12 digit PIN", Toast.LENGTH_LONG).show(); return;
                        }
                        voidLock.configure(first, second);
                        Toast.makeText(this, "Void Lock enabled", Toast.LENGTH_SHORT).show();
                    }).show();
            }).show();
    }

    private void showDrawer() { drawer.setVisibility(View.VISIBLE); search.setText(""); search.requestFocus(); }
    private void hideDrawer() {
        drawer.setVisibility(View.GONE);
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    @Override public void open(AppEntry app) {
        try {
            if (app.work) {
                LauncherApps service = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
                service.startMainActivity(app.component, app.user, null, null);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        } catch (Exception e) { Toast.makeText(this, "Could not open " + app.label, Toast.LENGTH_SHORT).show(); }
    }

    @Override public void menu(AppEntry app, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String profile = activeProfile == VoidLock.Profile.PRIVATE ? "private_" : "decoy_";
        boolean favorite = prefs.getStringSet(profile + "favorites", Collections.emptySet()).contains(app.component.flattenToString());
        menu.getMenu().add(favorite ? "Remove from dock" : "Add to dock");
        if (activeProfile == VoidLock.Profile.PRIVATE) {
            boolean decoy = prefs.getStringSet("decoy_allowed", Collections.emptySet()).contains(app.component.flattenToString());
            menu.getMenu().add(decoy ? "Remove from decoy" : "Show in decoy");
        }
        menu.getMenu().add("App info"); menu.getMenu().add("Hide app");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("dock")) toggleSet(profile + "favorites", app.component.flattenToString());
            else if (title.contains("decoy")) toggleSet("decoy_allowed", app.component.flattenToString());
            else if (title.equals("Hide app")) toggleSet(profile + "hidden", app.component.flattenToString());
            else if (app.work) {
                LauncherApps service = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
                service.startAppDetailsActivity(app.component, app.user, null, null);
            } else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + app.component.getPackageName())));
            loadApps(); return true;
        }); menu.show();
    }

    private void toggleSet(String key, String value) {
        Set<String> set = new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
        if (!set.add(value)) set.remove(value); prefs.edit().putStringSet(key, set).apply();
    }

    private void showSettings() {
        boolean workEnabled = prefs.getBoolean("work_profile_enabled", false);
        String[] items = {"Set as default launcher", "Configure Void Lock", "Lock now", "Create optional Work Profile",
            workEnabled ? "Hide Work Profile apps" : "Use existing Work Profile", "Change wallpaper", "Grid: 4 columns", "Grid: 5 columns", "Restore hidden apps"};
        new AlertDialog.Builder(this).setTitle("Void settings").setItems(items, (d, which) -> {
            if (which == 0) requestDefaultLauncher();
            else if (which == 1) showLockSetup();
            else if (which == 2) { activeProfile = null; hideDrawer(); Toast.makeText(this, "Void locked", Toast.LENGTH_SHORT).show(); }
            else if (which == 3) createWorkProfile();
            else if (which == 4) { prefs.edit().putBoolean("work_profile_enabled", !workEnabled).apply(); loadApps(); }
            else if (which == 5) startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            else if (which == 6 || which == 7) { prefs.edit().putInt("columns", which == 6 ? 4 : 5).apply(); recreate(); }
            else {
                String profile = activeProfile == VoidLock.Profile.PRIVATE ? "private_" : "decoy_";
                prefs.edit().remove(profile + "hidden").apply(); loadApps();
            }
        }).show();
    }

    private void createWorkProfile() {
        if (!getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_MANAGED_USERS)) {
            Toast.makeText(this, "This phone does not support Android Work Profiles", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            new ComponentName(this, VoidAdminReceiver.class));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "Work Profile setup is unavailable on this phone", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private void requestDefaultLauncher() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.app.role.RoleManager rm = (android.app.role.RoleManager)getSystemService(ROLE_SERVICE);
            if (rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME))
                startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), 7);
        } else startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
}
