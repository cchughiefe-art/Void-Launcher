package com.android.launcher3.agent;

import java.util.ArrayList;
import java.util.List;

/** Stable tab identifiers are independent of selection and list positions. */
public final class BrowserTabs {
    public static final int AGENT = 1;
    public static final int LIMIT = 8;
    public static final class Tab {
        public final int id;
        public String url, title;
        Tab(int id, String url, String title) { this.id = id; this.url = url; this.title = title; }
    }
    private final List<Tab> tabs = new ArrayList<>();
    private int selected = AGENT, next = 2;
    public BrowserTabs(String initial) { tabs.add(new Tab(AGENT, initial, "Page")); }
    public List<Tab> all() { return new ArrayList<>(tabs); }
    public Tab get(int id) { for (Tab t : tabs) if (t.id == id) return t; throw new IllegalArgumentException("Tab no longer exists"); }
    public int selected() { return selected; }
    public void select(int id) { get(id); selected = id; }
    public int add(String url) {
        if (tabs.size() >= LIMIT) throw new IllegalStateException("Close a tab before opening another (8-tab limit).");
        int id = next++; tabs.add(new Tab(id, url, "New tab")); selected = id; return id;
    }
    public boolean close(int id) {
        
        int index = -1; for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id == id) index = i;
        if (index < 0) return false;
        tabs.remove(index); if (tabs.isEmpty()) { int fresh=next++; tabs.add(new Tab(fresh, "about:blank", "New tab")); selected=fresh; return true; } if (selected == id) selected = tabs.get(Math.max(0, index - 1)).id; return true;
    }
}
