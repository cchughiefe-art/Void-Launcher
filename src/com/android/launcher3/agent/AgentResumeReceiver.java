package com.android.launcher3.agent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Launcher foreground entry resumes an enabled connection without opening browser UI. */
public class AgentResumeReceiver extends BroadcastReceiver {
    public static final String RESUME = "com.voidlauncher.agent.RESUME";
    @Override public void onReceive(Context c, Intent intent) {
        if (!RESUME.equals(intent.getAction())) return;
        AgentSettings settings = new AgentSettings(c);
        if (!settings.enabled()) return; // Setup and Stop always take precedence.
        try { c.startForegroundService(new Intent(c, AgentBrowserService.class)); }
        catch (RuntimeException e) { settings.status("Android deferred the connection. Open Agent Browser and tap Start."); }
    }
}
