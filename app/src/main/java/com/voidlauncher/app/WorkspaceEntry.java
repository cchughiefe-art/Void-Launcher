package com.voidlauncher.app;

import android.graphics.drawable.Drawable;

final class WorkspaceEntry {
    enum Type { APP, FOLDER }
    final Type type;
    final String id;
    final String label;
    final Drawable icon;
    final AppEntry app;

    private WorkspaceEntry(Type type, String id, String label, Drawable icon, AppEntry app) {
        this.type = type; this.id = id; this.label = label; this.icon = icon; this.app = app;
    }

    static WorkspaceEntry app(AppEntry app) {
        return new WorkspaceEntry(Type.APP, "A:" + app.component.flattenToString(), app.label, app.icon, app);
    }

    static WorkspaceEntry folder(String id, String label, Drawable icon) {
        return new WorkspaceEntry(Type.FOLDER, "F:" + id, label, icon, null);
    }
}
