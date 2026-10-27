package com.android.launcher3.agent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Resume only a connection the user left enabled. Never overrides Stop. */
public class AgentBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        AgentSettings s = new AgentSettings(c);
        if (!s.enabled() || !s.prefs.getBoolean("resume_boot", true)) return;
        try { c.startForegroundService(new Intent(c, AgentBrowserService.class)); }
        catch (RuntimeException e) { s.status("Android deferred restart. Tap Start in Agent Browser."); }
    }
}
