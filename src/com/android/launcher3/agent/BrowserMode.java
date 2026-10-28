package com.android.launcher3.agent;

/** Desktop layout request only; the mobile mode keeps the system WebView identity. */
public final class BrowserMode {
    private BrowserMode() {}
    public static String desktopUserAgent(String mobile) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Chrome/([0-9.]+)").matcher(mobile == null ? "" : mobile);
        String version = m.find() ? m.group(1) : "125.0.0.0";
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + version + " Safari/537.36";
    }
}
