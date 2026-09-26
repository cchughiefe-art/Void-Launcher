package com.voidlauncher.app;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

final class AppEntry {
    final String label;
    final ComponentName component;
    final Drawable icon;
    final UserHandle user;
    final boolean work;

    AppEntry(String label, ComponentName component, Drawable icon, UserHandle user, boolean work) {
        this.label = label;
        this.component = component;
        this.icon = icon;
        this.user = user;
        this.work = work;
    }
}
