package com.voidlauncher.app;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

public final class VoidAdminReceiver extends DeviceAdminReceiver {
    @Override public void onProfileProvisioningComplete(Context context, Intent intent) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, VoidAdminReceiver.class);
        manager.setProfileName(admin, "Void Private Space");
        manager.setProfileEnabled(admin);
        context.getSharedPreferences("void", Context.MODE_PRIVATE).edit().putBoolean("work_profile_enabled", true).apply();
    }
}
