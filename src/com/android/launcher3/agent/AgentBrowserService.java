package com.android.launcher3.agent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** User-visible, sticky connection. Stop always wins; uncertain navigation is never replayed. */
public class AgentBrowserService extends Service {
    public static final String STOP = "com.voidlauncher.agent.STOP";
    public void closeBrowser() {
        settings.prefs.edit().putBoolean("browser_open", false).commit();
        if (closeScreen != null) closeScreen.run();
        stopConnection();
    }
    public static final String OPEN_BROWSER = "com.voidlauncher.agent.OPEN_BROWSER";
    private static final String CLOSE_BROWSER = "com.voidlauncher.agent.CLOSE_BROWSER";
    private Runnable closeScreen;
    void setCloseScreen(Runnable callback) { closeScreen = callback; }
    private static final int ID = 8041;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private final LocalBinder binder = new LocalBinder();
    private AgentSettings settings;
    private AgentRelay relay;
    private AgentWebEngine engine;
    private AtomicFile receiptFile;
    private final Object receiptLock = new Object();
    private volatile boolean closed;
    private boolean session;
    private int failures;
    private long nextAttempt, lastDiscovery;
    private volatile long lastContact;
    long lastContact() { return lastContact; }
    private PowerManager.WakeLock commandWake;
    public class LocalBinder extends Binder { AgentBrowserService service() { return AgentBrowserService.this; } }
    AgentWebEngine engine() { return engine; }
    @Override public void onCreate() {
        super.onCreate(); settings = new AgentSettings(this); relay = new AgentRelay(settings);
        receiptFile = new AtomicFile(new File(getNoBackupFilesDir(), "agent-pending-result.json"));
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("void_agent", "Agent Browser connection", NotificationManager.IMPORTANCE_LOW));
        foreground(settings.enabled() ? "Starting browser connection…" : "Browser open · connection off");
        try { engine = new AgentWebEngine(getApplicationContext(), settings); }
        catch (RuntimeException e) { settings.enabled(false); settings.status("WebView could not start. Update Android System WebView, then open Agent Browser."); stopSelf(); return; }
        io.scheduleWithFixedDelay(this::tick, 0, 1, TimeUnit.SECONDS);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && CLOSE_BROWSER.equals(intent.getAction())) {
            closeBrowser(); return START_NOT_STICKY;
        }
        if (intent != null && OPEN_BROWSER.equals(intent.getAction())) {
            settings.prefs.edit().putBoolean("browser_open", true).commit();
        }
        if (intent != null && STOP.equals(intent.getAction())) {
            stopConnection();
            return settings.prefs.getBoolean("browser_open", false) ? START_STICKY : START_NOT_STICKY;
        }
        if (!settings.enabled()) {
            if (settings.prefs.getBoolean("browser_open", false)) {
                foreground("Browser open · connection off"); return START_STICKY;
            }
            stopSelf(); return START_NOT_STICKY;
        }
        if (closed) { closed = false; session = false; nextAttempt = 0; }
        return START_STICKY;
    }
    @Override public IBinder onBind(Intent intent) { return binder; }
    private void foreground(String text) {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, AgentBrowserActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, AgentBrowserService.class).setAction(STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent close = PendingIntent.getService(this, 2, new Intent(this, AgentBrowserService.class).setAction(CLOSE_BROWSER), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, "void_agent").setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("VOID Agent Browser").setContentText(text).setContentIntent(open)
                .setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE)
                .addAction(new Notification.Action.Builder(null, "Reveal browser", open).build())
                .addAction(new Notification.Action.Builder(null, "Stop agent", stop).build())
                .addAction(new Notification.Action.Builder(null, "Close browser", close).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE); else startForeground(ID, n);
    }
    private void status(String text) { settings.status(text); main.post(() -> { if (!closed) foreground(text); }); }
    private void tick() {
        if (closed || !settings.enabled() || engine == null || System.currentTimeMillis() < nextAttempt) return;
        try {
            if (!session) {
                if (System.currentTimeMillis() - lastDiscovery > 10000) { relay.discover(); lastDiscovery = System.currentTimeMillis(); }
                relay.call("/device/session", new JSONObject().put("enabled", true).put("untilStopped", true));
                lastContact = System.currentTimeMillis(); session = true; status("Connected · waiting for commands");
            }
            // Recover a completed receipt first. Never repeat the browser action after a restart.
            if (hasReceipt()) { deliverPending(); if (hasReceipt()) return; }
            String uncertain = settings.prefs.getString("inflight", "");
            if (!uncertain.isEmpty()) {
                saveReceipt(uncertain, AgentWebEngine.error("Phone process interrupted this command. It was not retried."));
                deliverPending(); return;
            }
            JSONObject next = relay.call("/device/next", null);
            lastContact = System.currentTimeMillis();
            if (next.optBoolean("expired")) { session = false; return; }
            JSONObject command = next.optJSONObject("command");
            if (command == null) { failures = 0; return; }
            String id = command.optString("id");
            if (!id.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Invalid command ID");
            if (id.equals(settings.prefs.getString("completed", ""))) {
                saveReceipt(id, AgentWebEngine.error("Duplicate command was not executed.")); deliverPending(); return;
            }
            settings.prefs.edit().putString("inflight", id).commit();
            if (closed || !settings.enabled()) return;
            status("Reading requested page…");
            PowerManager pm = getSystemService(PowerManager.class);
            commandWake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":agent-command"); commandWake.acquire(30000);
            JSONObject result;
            try { result = engine.execute(command).get(24, TimeUnit.SECONDS); }
            catch (Exception e) { result = AgentWebEngine.error("Browser command interrupted or timed out. It was not retried."); main.post(() -> engine.stopCommands()); }
            finally { if (commandWake.isHeld()) commandWake.release(); }
            if (closed || !settings.enabled()) return;
            saveReceipt(id, result); deliverPending(); failures = 0;
            if (result.optBoolean("blocked")) { main.post(this::stopConnection); return; }
            status(result.has("error") ? "Command stopped · open browser for details" : "Connected · result received");
        } catch (Exception e) {
            session = false; failures = Math.min(failures + 1, 6);
            nextAttempt = System.currentTimeMillis() + Math.min(60000, 1000L << failures);
            status(e instanceof AgentRelay.RelayException && ((AgentRelay.RelayException)e).code == 401
                    ? "Device key rejected · open Agent Browser settings" : "Connection interrupted · retrying with backoff");
        }
    }
    private boolean hasReceipt() {
        synchronized (receiptLock) {
            return receiptFile.getBaseFile().exists() || new File(receiptFile.getBaseFile().getPath() + ".bak").exists();
        }
    }
    private void saveReceipt(String id, JSONObject result) throws Exception {
        synchronized (receiptLock) {
        if (closed || !settings.enabled()) return;
        JSONObject saved = new JSONObject().put("id", id).put("result", result);
        java.io.FileOutputStream out = receiptFile.startWrite();
        try { out.write(saved.toString().getBytes(StandardCharsets.UTF_8)); receiptFile.finishWrite(out); }
        catch (Exception e) { receiptFile.failWrite(out); throw e; }
        }
    }
    private void deliverPending() throws Exception {
        JSONObject r;
        synchronized (receiptLock) { r = new JSONObject(new String(receiptFile.readFully(), StandardCharsets.UTF_8)); }
        String id = r.getString("id");
        if (!id.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Invalid saved receipt");
        try { relay.call("/results/" + id, r.getJSONObject("result")); }
        catch (AgentRelay.RelayException e) {
            if (e.code != 409) throw e;
            // Relay restart/Stop invalidates the lease. Do not repeat the action.
            settings.status("Previous command lease ended; action was not repeated.");
        }
        synchronized (receiptLock) { settings.prefs.edit().putString("completed", id).remove("inflight").commit(); receiptFile.delete(); }
    }
    public void stopConnection() {
        settings.enabled(false); closed = true;
        if (engine != null) engine.stopCommands();
        // Clear local intent synchronously before any asynchronous network operation.
        synchronized (receiptLock) { settings.prefs.edit().remove("inflight").commit(); receiptFile.delete(); }
        settings.status("Stopped by you");
        io.execute(() -> { try { relay.call("/device/session", new JSONObject().put("enabled", false)); } catch (Exception ignored) {} });
        if (settings.prefs.getBoolean("browser_open", false)) {
            foreground("Browser open · agent stopped");
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
        }
    }
    @Override public void onDestroy() {
        closed = true; if (engine != null) engine.destroy();
        if (commandWake != null && commandWake.isHeld()) commandWake.release();
        io.shutdown(); super.onDestroy();
    }
    @Override public void onTaskRemoved(Intent rootIntent) {
        // Swiping away the screen keeps an explicitly enabled connection; Stop/force-stop does not.
        super.onTaskRemoved(rootIntent);
    }
}

