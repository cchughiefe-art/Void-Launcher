package com.voidlauncher.app;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class MainActivity extends AppCompatActivity implements AppAdapter.Listener, WorkspaceAdapter.Listener, SensorEventListener {
    private enum Space { MAIN, PRIVATE }
    private final List<AppEntry> allApps = new ArrayList<>();
    private static final List<AppEntry> PRIVATE_CACHE = new ArrayList<>();
    private static final List<AppEntry> PRIVATE_SPACE_CACHE = new ArrayList<>();
    static void clearAppCaches() { PRIVATE_CACHE.clear(); PRIVATE_SPACE_CACHE.clear(); }
    private AppAdapter adapter;
    private WorkspaceAdapter homeAdapter;
    private RecyclerView homeGrid;
    private View drawer;
    private EditText search;
    private LinearLayout dock;
    private View settingsButton;
    private ParallaxWallpaperView wallpaper;
    private ParallaxWallpaperView wallpaperForeground;
    private View wallpaperDim;
    private SensorManager sensorManager;
    private Sensor motionSensor;
    private SharedPreferences prefs;
    private Space activeProfile = Space.MAIN;
    private float touchDownY;
    private float touchDownX;
    private int gesturePointers;
    private float drawerTouchY;
    private BroadcastReceiver packageReceiver;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        prefs = getSharedPreferences("void", MODE_PRIVATE);
        drawer = findViewById(R.id.drawer);
        search = findViewById(R.id.search);
        dock = findViewById(R.id.dock);
        settingsButton = findViewById(R.id.settings);
        wallpaper = findViewById(R.id.wallpaper);
        wallpaperForeground = findViewById(R.id.wallpaperForeground);
        wallpaperDim = findViewById(R.id.wallpaperDim);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        RecyclerView grid = findViewById(R.id.apps);
        grid.setLayoutManager(new GridLayoutManager(this, prefs.getInt("columns", 4)));
        adapter = new AppAdapter(this);
        grid.setAdapter(adapter);
        homeGrid = findViewById(R.id.homeApps);
        homeGrid.setLayoutManager(new GridLayoutManager(this, prefs.getInt("home_columns", 4)));
        homeAdapter = new WorkspaceAdapter(this);
        homeGrid.setAdapter(homeAdapter);
        ItemTouchHelper dragHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override public boolean isLongPressDragEnabled() { return !prefs.getBoolean("lock_layout", false); }
            @Override public boolean onMove(RecyclerView list, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
                homeAdapter.move(from.getBindingAdapterPosition(), to.getBindingAdapterPosition()); return true;
            }
            @Override public void onSwiped(RecyclerView.ViewHolder holder, int direction) {}
        });
        dragHelper.attachToRecyclerView(homeGrid);

        findViewById(R.id.openDrawer).setOnClickListener(v -> { activeProfile = Space.MAIN; safeLoadApps(); showDrawer(); });
        settingsButton.setOnClickListener(v -> handleUtilityButton());
        findViewById(R.id.homeScreen).setOnTouchListener(this::handleHomeGesture);
        findViewById(R.id.homeScreen).setOnLongClickListener(v -> { showHomeMenu(v); return true; });
        homeGrid.setOnTouchListener(this::handleHomeGesture);
        grid.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) drawerTouchY = event.getY();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float distance = event.getY() - drawerTouchY;
                if (distance >= 48 * getResources().getDisplayMetrics().density && !grid.canScrollVertically(-1)) {
                    returnHome(); return true;
                }
            }
            return false;
        });
        search.addTextChangedListener(new SimpleTextWatcher(this::filter));
        search.setOnEditorActionListener((v, action, event) -> {
            AppEntry first = adapter.first(); if (first != null) open(first); return true;
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if (drawer.getVisibility() == View.VISIBLE) hideDrawer(); }
        });
        safeLoadApps();
        registerPackageReceiver();
        if (!prefs.getBoolean("default_prompted", false)) {
            prefs.edit().putBoolean("default_prompted", true).apply();
            findViewById(R.id.root).postDelayed(this::requestDefaultLauncher, 650);
        }
    }

    @Override protected void onDestroy() {
        if (packageReceiver != null) try { unregisterReceiver(packageReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override protected void onResume() {
        super.onResume();
        RecyclerView drawerGrid = findViewById(R.id.apps);
        if (drawerGrid.getLayoutManager() instanceof GridLayoutManager)
            ((GridLayoutManager)drawerGrid.getLayoutManager()).setSpanCount(prefs.getInt("columns", 4));
        if (homeGrid != null && homeGrid.getLayoutManager() instanceof GridLayoutManager)
            ((GridLayoutManager)homeGrid.getLayoutManager()).setSpanCount(prefs.getInt("home_columns", 4));
        if (motionSensor != null && prefs.getBoolean("parallax_enabled", true) && !prefs.getBoolean("reduce_motion", false))
            sensorManager.registerListener(this, motionSensor, SensorManager.SENSOR_DELAY_UI);
        if (adapter != null) safeLoadApps();
    }

    @Override protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override protected void onStop() {
        super.onStop();
        if (prefs.getBoolean("lock_on_leave", true)) activeProfile = Space.MAIN;
        if (prefs.getBoolean("clear_search", true) && search != null) search.setText("");
        if (drawer != null && !prefs.getBoolean("keep_drawer", true)) drawer.setVisibility(View.GONE);
    }

    private void loadApps() {
        boolean mainSpace = activeProfile == Space.MAIN;
        String profile = mainSpace ? "main_" : "private_";
        Set<String> hidden = prefs.getStringSet(profile + "hidden", Collections.emptySet());
        List<AppEntry> cache = mainSpace ? PRIVATE_CACHE : PRIVATE_SPACE_CACHE;
        if (prefs.getBoolean("cache_apps", true) && !cache.isEmpty()) {
            showLoadedApps(cache, mainSpace);
            return;
        }
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = getPackageManager().queryIntentActivities(launcherIntent, 0);
        if (!prefs.getBoolean("private_initialized", false)) {
            Set<String> starter = new HashSet<>();
            for (ResolveInfo info : infos) if (safePrivateDefault(info))
                starter.add(new android.content.ComponentName(info.activityInfo.packageName, info.activityInfo.name).flattenToString());
            prefs.edit().putStringSet("private_allowed", starter).putBoolean("private_initialized", true).apply();
        }
        Set<String> privateAllowed = prefs.getStringSet("private_allowed", Collections.emptySet());
        allApps.clear();
        for (ResolveInfo info : infos) {
            android.content.ComponentName component = new android.content.ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            if (component.getPackageName().equals(getPackageName())) continue;
            if (hidden.contains(component.flattenToString())) continue;
            if (!mainSpace && !privateAllowed.contains(component.flattenToString())) continue;
            String label = info.loadLabel(getPackageManager()).toString();
            allApps.add(new AppEntry(label, component, info.loadIcon(getPackageManager()), Process.myUserHandle(), false));
        }
        allApps.sort((a,b) -> prefs.getBoolean("sort_descending", false)
            ? b.label.compareToIgnoreCase(a.label) : a.label.compareToIgnoreCase(b.label));
        if (prefs.getBoolean("cache_apps", true)) { cache.clear(); cache.addAll(allApps); }
        showLoadedApps(new ArrayList<>(allApps), mainSpace);
    }

    private void safeLoadApps() {
        try { loadApps(); }
        catch (Exception error) {
            allApps.clear();
            adapter.submit(allApps);
            updateUtilityButton(activeProfile == Space.MAIN);
            applyAppearance(activeProfile == Space.MAIN);
        }
    }

    private void showLoadedApps(List<AppEntry> apps, boolean mainSpace) {
        allApps.clear(); allApps.addAll(apps);
        adapter.setShowLabels(prefs.getBoolean("show_labels", true));
        adapter.setIconSize(prefs.getInt("drawer_icon_size", 56));
        adapter.submit(allApps);
        updateUtilityButton(mainSpace);
        applyAppearance(mainSpace);
        buildDock();
        if (mainSpace) buildHomeApps();
    }

    private boolean handleHomeGesture(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownY = event.getRawY(); touchDownX = event.getRawX(); gesturePointers = 1;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)
            gesturePointers = Math.max(gesturePointers, event.getPointerCount());
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float density = getResources().getDisplayMetrics().density;
            float width = getResources().getDisplayMetrics().widthPixels;
            float height = getResources().getDisplayMetrics().heightPixels;
            float dx = event.getRawX() - touchDownX, dy = event.getRawY() - touchDownY;
            float edge = 42 * density, horizontal = 64 * density, vertical = 48 * density;
            boolean leftEdge = touchDownX <= edge && dx >= horizontal;
            boolean rightEdge = touchDownX >= width - edge && dx <= -horizontal;
            if ((leftEdge || rightEdge) && prefs.getBoolean("edge_private_swipe", true)) {
                openPrivateSpace(); return true;
            }
            if (touchDownY >= height * 0.55f && dy <= -vertical && prefs.getBoolean("bottom_drawer_swipe", true)) {
                activeProfile = Space.MAIN; safeLoadApps(); showDrawer(); return true;
            }
        }
        return false;
    }

    private void applyAppearance(boolean mainSpace) {
        boolean wallpaperEnabled = prefs.getBoolean("wallpaper_enabled", true);
        wallpaperDim.setVisibility(wallpaperEnabled && prefs.getBoolean("wallpaper_dim", true) ? View.VISIBLE : View.GONE);
        wallpaperDim.setAlpha(prefs.getInt("wallpaper_dim_strength", 40) / 100f);
        String saved = prefs.getString(mainSpace ? "main_wallpaper" : "private_wallpaper", "");
        if (!wallpaperEnabled) {
            wallpaper.setImageDrawable(null); wallpaper.setVisibility(View.GONE);
        } else if (!saved.isEmpty()) {
            wallpaper.setVisibility(View.VISIBLE);
            try {
                int preset = presetResource(saved);
                if (preset != 0) wallpaper.setImageResource(preset); else wallpaper.setImageURI(Uri.parse(saved));
            }
            catch (Exception ignored) { wallpaper.setVisibility(View.GONE); }
        } else {
            wallpaper.setVisibility(View.VISIBLE);
            wallpaper.setImageResource(R.drawable.wallpaper_void);
        }
        String focus = prefs.getString("wallpaper_focus", "center");
        float focalY = focus.equals("top") ? 0f : focus.equals("bottom") ? 1f : 0.5f;
        wallpaper.configure(prefs.getInt("wallpaper_zoom", 110) / 100f, focalY);
        wallpaper.setMotion(0f, 0f);
        applyDepthLayer(mainSpace, wallpaperEnabled, focalY);
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            if (prefs.getBoolean("wallpaper_blur", false)) wallpaper.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(12f, 12f, android.graphics.Shader.TileMode.CLAMP));
            else wallpaper.setRenderEffect(null);
        }
        dock.setAlpha(prefs.getBoolean("transparent_dock", true) ? 0.88f : 1f);
        dock.setBackgroundTintList(null);
    }

    private void applyDepthLayer(boolean mainSpace, boolean wallpaperEnabled, float focalY) {
        String path = prefs.getString(mainSpace ? "depth_main_path" : "depth_private_path", "");
        java.io.File file = new java.io.File(path);
        if (!wallpaperEnabled || !prefs.getBoolean("depth_effect", true) || path.isEmpty() || !file.exists()) {
            wallpaperForeground.setImageDrawable(null); wallpaperForeground.setVisibility(View.GONE); return;
        }
        wallpaperForeground.setVisibility(View.VISIBLE);
        wallpaperForeground.setImageURI(Uri.fromFile(file));
        wallpaperForeground.configure(prefs.getInt("wallpaper_zoom", 110) / 100f, focalY);
        wallpaperForeground.setMotion(0f, 0f);
    }

    private int presetResource(String saved) {
        if ("builtin:void".equals(saved)) return R.drawable.wallpaper_void;
        if ("builtin:blue".equals(saved)) return R.drawable.wallpaper_pixel_blue;
        if ("builtin:sunrise".equals(saved)) return R.drawable.wallpaper_sunrise;
        if ("builtin:emerald".equals(saved)) return R.drawable.wallpaper_emerald;
        return 0;
    }

    private boolean safePrivateDefault(ResolveInfo info) {
        String value = (info.loadLabel(getPackageManager()) + " " + info.activityInfo.packageName).toLowerCase(Locale.ROOT);
        String[] safe = {"camera", "files", "gallery", "photos", "chrome", "browser"};
        for (String word : safe) if (value.contains(word)) return true;
        return false;
    }

    private void handleUtilityButton() {
        Intent intent = new Intent(this, activeProfile == Space.MAIN ? SettingsActivity.class : PrivateBrowserActivity.class);
        startActivity(intent);
    }

    private void openPrivateSpace() {
        PrivateAuth.authenticate(this, prefs, () -> {
            activeProfile = Space.PRIVATE;
            safeLoadApps();
            showDrawer();
        });
    }

    private void updateUtilityButton(boolean mainSpace) {
        settingsButton.setVisibility(View.VISIBLE);
        android.widget.TextView button = (android.widget.TextView) settingsButton;
        button.setText(mainSpace ? "⚙" : "◉");
        button.setContentDescription(mainSpace ? "Void settings" : "Open Private Browser");
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
        String profile = activeProfile == Space.MAIN ? "main_" : "private_";
        List<String> order = readOrder(profile + "dock_order");
        Set<String> favorites = prefs.getStringSet(profile + "favorites", Collections.emptySet());
        if (order.isEmpty() && !favorites.isEmpty()) {
            for (AppEntry app : allApps) if (favorites.contains(app.component.flattenToString())) order.add(app.component.flattenToString());
            writeOrder(profile + "dock_order", order);
        }
        List<AppEntry> chosen = new ArrayList<>();
        for (String component : order) { AppEntry app = findApp(component); if (app != null) chosen.add(app); }
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

    private void buildHomeApps() {
        migrateLegacyHome();
        List<WorkspaceEntry> chosen = new ArrayList<>();
        for (String token : readOrder("main_home_order")) {
            if (token.startsWith("A:")) {
                AppEntry app = findApp(token.substring(2));
                if (app != null) chosen.add(WorkspaceEntry.app(app));
            } else if (token.startsWith("F:")) {
                String id = token.substring(2);
                Set<String> members = prefs.getStringSet("folder_" + id + "_apps", Collections.emptySet());
                if (!members.isEmpty()) chosen.add(WorkspaceEntry.folder(id,
                    prefs.getString("folder_" + id + "_name", "Folder"), folderIcon(members)));
            }
        }
        homeAdapter.setAppearance(prefs.getBoolean("show_home_labels", true), prefs.getInt("home_icon_size", 56));
        homeAdapter.submit(chosen.subList(0, Math.min(32, chosen.size())));
    }

    private void migrateLegacyHome() {
        if (!readOrder("main_home_order").isEmpty()) return;
        Set<String> old = prefs.getStringSet("main_home", Collections.emptySet());
        if (old.isEmpty()) return;
        List<String> order = new ArrayList<>();
        for (AppEntry app : allApps) if (old.contains(app.component.flattenToString())) order.add("A:" + app.component.flattenToString());
        writeOrder("main_home_order", order); prefs.edit().remove("main_home").apply();
    }

    private AppEntry findApp(String flattened) {
        for (AppEntry app : allApps) if (app.component.flattenToString().equals(flattened)) return app;
        return null;
    }

    private android.graphics.drawable.Drawable folderIcon(Set<String> members) {
        List<android.graphics.drawable.Drawable> icons = new ArrayList<>();
        for (AppEntry app : allApps) if (members.contains(app.component.flattenToString())) icons.add(app.icon);
        return new FolderIconDrawable(icons);
    }

    private List<String> readOrder(String key) {
        String value = prefs.getString(key, ""); List<String> result = new ArrayList<>();
        if (!value.isEmpty()) for (String token : value.split("\\n")) if (!token.trim().isEmpty()) result.add(token.trim());
        return result;
    }

    private void writeOrder(String key, List<String> order) {
        StringBuilder value = new StringBuilder();
        for (String token : order) { if (value.length() > 0) value.append('\n'); value.append(token); }
        prefs.edit().putString(key, value.toString()).apply();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR || wallpaper == null) return;
        float[] rotation = new float[9], orientation = new float[3];
        SensorManager.getRotationMatrixFromVector(rotation, event.values);
        SensorManager.getOrientation(rotation, orientation);
        float strength = prefs.getInt("parallax_strength", 18);
        float limit = strength * getResources().getDisplayMetrics().density;
        wallpaper.setMotion(Math.max(-limit, Math.min(limit, -orientation[2] * limit)),
            Math.max(-limit, Math.min(limit, -orientation[1] * limit)));
        if (wallpaperForeground != null && wallpaperForeground.getVisibility() == View.VISIBLE)
            wallpaperForeground.setMotion(Math.max(-limit, Math.min(limit, -orientation[2] * limit)),
                Math.max(-limit, Math.min(limit, -orientation[1] * limit)));
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void showDrawer() {
        search.setText(""); search.clearFocus();
        drawer.setVisibility(View.VISIBLE);
        if (prefs.getBoolean("reduce_motion", false)) { drawer.setAlpha(1f); drawer.setTranslationY(0f); return; }
        drawer.setAlpha(0f); drawer.setTranslationY(72f * getResources().getDisplayMetrics().density);
        drawer.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }
    private void hideDrawer() {
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
        search.clearFocus();
        if (prefs.getBoolean("reduce_motion", false)) { drawer.setVisibility(View.GONE); return; }
        drawer.animate().alpha(0f).translationY(64f * getResources().getDisplayMetrics().density).setDuration(180)
            .withEndAction(() -> { drawer.setVisibility(View.GONE); drawer.setAlpha(1f); drawer.setTranslationY(0f); }).start();
    }
    private void returnHome() {
        activeProfile = Space.MAIN;
        safeLoadApps();
        hideDrawer();
    }

    @Override public void open(AppEntry app) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        } catch (Exception e) { Toast.makeText(this, "Could not open " + app.label, Toast.LENGTH_SHORT).show(); }
    }

    @Override public void menu(AppEntry app, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        String profile = activeProfile == Space.MAIN ? "main_" : "private_";
        boolean favorite = readOrder(profile + "dock_order").contains(app.component.flattenToString())
            || prefs.getStringSet(profile + "favorites", Collections.emptySet()).contains(app.component.flattenToString());
        menu.getMenu().add(favorite ? "Remove from dock" : "Add to dock");
        if (activeProfile == Space.MAIN) {
            boolean onHome = readOrder("main_home_order").contains("A:" + app.component.flattenToString())
                || prefs.getStringSet("main_home", Collections.emptySet()).contains(app.component.flattenToString());
            menu.getMenu().add(onHome ? "Remove from Home" : "Add to Home");
            menu.getMenu().add("Create folder with…");
            boolean inPrivate = prefs.getStringSet("private_allowed", Collections.emptySet()).contains(app.component.flattenToString());
            menu.getMenu().add(inPrivate ? "Remove from Private Space" : "Add to Private Space");
        }
        menu.getMenu().add("App info");
        menu.getMenu().add("Open in Play Store");
        menu.getMenu().add("Share app");
        menu.getMenu().add("Uninstall app");
        menu.getMenu().add("Hide app");
        List<ShortcutInfo> shortcuts = queryShortcuts(app);
        for (int i = 0; i < shortcuts.size() && i < 4; i++)
            menu.getMenu().add(0, 1000 + i, i, shortcuts.get(i).getShortLabel());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() >= 1000 && item.getItemId() < 1000 + shortcuts.size()) {
                openShortcut(shortcuts.get(item.getItemId() - 1000)); return true;
            }
            String title = item.getTitle().toString();
            if (title.contains("dock")) toggleDock(profile, app.component.flattenToString());
            else if (title.contains("Home")) toggleHome(app.component.flattenToString());
            else if (title.equals("Create folder with…")) chooseFolderPartner(app);
            else if (title.contains("Private Space")) toggleSet("private_allowed", app.component.flattenToString());
            else if (title.equals("Hide app")) toggleSet(profile + "hidden", app.component.flattenToString());
            else if (title.equals("App info")) openAppInfo(app);
            else if (title.equals("Open in Play Store")) openStore(app);
            else if (title.equals("Share app")) shareApp(app);
            else if (title.equals("Uninstall app")) uninstallApp(app);
            safeLoadApps(); return true;
        }); menu.show();
    }

    private List<ShortcutInfo> queryShortcuts(AppEntry app) {
        if (android.os.Build.VERSION.SDK_INT < 25) return Collections.emptyList();
        try {
            LauncherApps launcher = (LauncherApps)getSystemService(LAUNCHER_APPS_SERVICE);
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery().setPackage(app.component.getPackageName())
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST |
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<ShortcutInfo> result = launcher.getShortcuts(query, Process.myUserHandle());
            return result == null ? Collections.emptyList() : result;
        } catch (Exception ignored) { return Collections.emptyList(); }
    }

    private void openShortcut(ShortcutInfo shortcut) {
        try {
            LauncherApps launcher = (LauncherApps)getSystemService(LAUNCHER_APPS_SERVICE);
            launcher.startShortcut(shortcut.getPackage(), shortcut.getId(), null, null, shortcut.getUserHandle());
        } catch (Exception error) { Toast.makeText(this, "Could not open this shortcut", Toast.LENGTH_SHORT).show(); }
    }

    private void openAppInfo(AppEntry app) {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + app.component.getPackageName())));
    }

    private void uninstallApp(AppEntry app) {
        try { startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.component.getPackageName()))); }
        catch (Exception error) { Toast.makeText(this, "This app cannot be uninstalled", Toast.LENGTH_SHORT).show(); }
    }

    private void openStore(AppEntry app) {
        String packageName = app.component.getPackageName();
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName))); }
        catch (Exception error) { startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=" + packageName))); }
    }

    private void shareApp(AppEntry app) {
        String link = "https://play.google.com/store/apps/details?id=" + app.component.getPackageName();
        Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, app.label)
            .putExtra(Intent.EXTRA_TEXT, app.label + "\n" + link);
        startActivity(Intent.createChooser(share, "Share " + app.label));
    }

    private void toggleSet(String key, String value) {
        Set<String> set = new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
        if (!set.add(value)) set.remove(value); prefs.edit().putStringSet(key, set).apply();
        clearAppCaches();
    }

    private void toggleDock(String profile, String component) {
        List<String> order = readOrder(profile + "dock_order");
        if (order.contains(component)) order.remove(component);
        else if (order.size() >= prefs.getInt("dock_size", 5)) {
            Toast.makeText(this, "Dock is full", Toast.LENGTH_SHORT).show(); return;
        } else order.add(component);
        writeOrder(profile + "dock_order", order); prefs.edit().remove(profile + "favorites").apply(); buildDock();
    }

    private void toggleHome(String value) {
        migrateLegacyHome(); List<String> order = readOrder("main_home_order"); String token = "A:" + value;
        if (order.contains(token)) order.remove(token);
        else if (order.size() >= 32) { Toast.makeText(this, "Home screen is full", Toast.LENGTH_SHORT).show(); return; }
        else order.add(token);
        writeOrder("main_home_order", order);
        buildHomeApps();
    }

    @Override public void openWorkspaceItem(WorkspaceEntry item) {
        if (item.type == WorkspaceEntry.Type.APP) open(item.app); else openFolder(item.id);
    }

    @Override public void menuWorkspaceItem(WorkspaceEntry item, View anchor) {
        if (prefs.getBoolean("lock_layout", false)) {
            Toast.makeText(this, "Unlock the Home layout in Settings to edit it", Toast.LENGTH_SHORT).show(); return;
        }
        if (item.type == WorkspaceEntry.Type.APP) menu(item.app, anchor); else folderMenu(item.id, anchor);
    }

    @Override public void workspaceOrderChanged(List<WorkspaceEntry> items) {
        List<String> order = new ArrayList<>(); for (WorkspaceEntry item : items) order.add(item.id);
        writeOrder("main_home_order", order);
    }

    private void chooseFolderPartner(AppEntry first) {
        List<AppEntry> candidates = new ArrayList<>();
        for (AppEntry app : allApps) if (!app.component.equals(first.component)) candidates.add(app);
        String[] labels = new String[candidates.size()]; for (int i = 0; i < candidates.size(); i++) labels[i] = candidates.get(i).label;
        new AlertDialog.Builder(this).setTitle("Create folder with " + first.label)
            .setItems(labels, (dialog, which) -> createFolder(first, candidates.get(which))).show();
    }

    private void createFolder(AppEntry first, AppEntry second) {
        String id = UUID.randomUUID().toString(); Set<String> members = new HashSet<>();
        members.add(first.component.flattenToString()); members.add(second.component.flattenToString());
        List<String> order = readOrder("main_home_order");
        order.remove("A:" + first.component.flattenToString()); order.remove("A:" + second.component.flattenToString());
        order.add("F:" + id);
        prefs.edit().putStringSet("folder_" + id + "_apps", members)
            .putString("folder_" + id + "_name", "Folder").apply();
        writeOrder("main_home_order", order); buildHomeApps();
    }

    private void openFolder(String id) {
        Set<String> memberIds = prefs.getStringSet("folder_" + id + "_apps", Collections.emptySet());
        List<AppEntry> members = new ArrayList<>(); for (AppEntry app : allApps) if (memberIds.contains(app.component.flattenToString())) members.add(app);
        String[] labels = new String[members.size()]; for (int i = 0; i < members.size(); i++) labels[i] = members.get(i).label;
        new AlertDialog.Builder(this).setTitle(prefs.getString("folder_" + id + "_name", "Folder"))
            .setItems(labels, (dialog, which) -> open(members.get(which))).setNegativeButton("Close", null).show();
    }

    private void folderMenu(String id, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor); popup.getMenu().add("Rename folder");
        popup.getMenu().add("Add apps"); popup.getMenu().add("Delete folder");
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Rename folder")) renameFolder(id); else if (title.equals("Add apps")) addAppToFolder(id); else deleteFolder(id);
            return true;
        }); popup.show();
    }

    private void renameFolder(String id) {
        EditText input = new EditText(this); input.setSingleLine(true); input.setText(prefs.getString("folder_" + id + "_name", "Folder"));
        new AlertDialog.Builder(this).setTitle("Rename folder").setView(input).setPositiveButton("Save", (d, w) -> {
            String name = input.getText().toString().trim(); if (name.isEmpty()) name = "Folder";
            prefs.edit().putString("folder_" + id + "_name", name).apply(); buildHomeApps();
        }).setNegativeButton("Cancel", null).show();
    }

    private void addAppToFolder(String id) {
        String[] labels = new String[allApps.size()]; for (int i = 0; i < allApps.size(); i++) labels[i] = allApps.get(i).label;
        new AlertDialog.Builder(this).setTitle("Add app to folder").setItems(labels, (d, which) -> {
            Set<String> members = new HashSet<>(prefs.getStringSet("folder_" + id + "_apps", Collections.emptySet()));
            String component = allApps.get(which).component.flattenToString(); members.add(component);
            List<String> order = readOrder("main_home_order"); order.remove("A:" + component);
            prefs.edit().putStringSet("folder_" + id + "_apps", members).apply(); writeOrder("main_home_order", order); buildHomeApps();
        }).show();
    }

    private void deleteFolder(String id) {
        List<String> order = readOrder("main_home_order"); order.remove("F:" + id); writeOrder("main_home_order", order);
        prefs.edit().remove("folder_" + id + "_apps").remove("folder_" + id + "_name").apply(); buildHomeApps();
    }

    private void showHomeMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        boolean locked = prefs.getBoolean("lock_layout", false);
        popup.getMenu().add(locked ? "Unlock Home layout" : "Lock Home layout");
        popup.getMenu().add("Launcher settings"); popup.getMenu().add("Wallpaper picker"); popup.getMenu().add("Open app drawer");
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("Home layout")) {
                prefs.edit().putBoolean("lock_layout", !locked).apply();
                Toast.makeText(this, locked ? "Home layout unlocked" : "Home layout locked", Toast.LENGTH_SHORT).show();
            } else if (title.equals("Launcher settings")) startActivity(new Intent(this, SettingsActivity.class));
            else if (title.equals("Wallpaper picker")) startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            else { activeProfile = Space.MAIN; safeLoadApps(); showDrawer(); }
            return true;
        }); popup.show();
    }

    private void registerPackageReceiver() {
        packageReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                clearAppCaches(); if (homeGrid != null) homeGrid.postDelayed(() -> safeLoadApps(), 250);
            }
        };
        IntentFilter filter = new IntentFilter(); filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED); filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED); filter.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(packageReceiver, filter);
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }

    private void requestDefaultLauncher() {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.app.role.RoleManager rm = (android.app.role.RoleManager)getSystemService(ROLE_SERVICE);
            if (rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME))
                startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), 7);
        } else startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
    }
}
