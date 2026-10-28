package com.android.launcher3.agent;

public final class BrowserModeTest {
    public static void main(String[] args) {
        String desktop=BrowserMode.desktopUserAgent("Mozilla/5.0 (Linux; Android 15; wv) Chrome/141.0.7390.122 Mobile Safari/537.36");
        if(!desktop.contains("Chrome/141.0.7390.122")||desktop.contains("Android")||desktop.contains("Mobile"))throw new AssertionError("Desktop identity must preserve engine version");
        BrowserTabs tabs=new BrowserTabs("https://x.com");int first=tabs.selected();tabs.get(first).desktop=true;
        int second=tabs.add("https://rarelist.io");if(tabs.get(second).desktop)throw new AssertionError("New tab must default to mobile");
        tabs.select(first);if(!tabs.get(first).desktop)throw new AssertionError("Mode must survive tab selection");
        if(BrowserAddress.canRemember("https://rarelist.io/join?auth=credential"))throw new AssertionError("Callback credentials must not enter history or tab receipts");
        System.out.println("Browser mode and callback privacy checks passed");
    }
}
