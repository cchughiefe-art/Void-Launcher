package com.android.launcher3.agent;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class BrowserAddress {
    private BrowserAddress() {}
    public static String resolve(String input) {
        String s = input.trim(); if (s.isEmpty()) return "about:blank";
        if (s.equals("about:blank")) return s;
        if (s.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) return AgentPolicy.browserUrl(s);
        if (!s.contains(" ") && s.contains(".")) return AgentPolicy.browserUrl("https://" + s);
        try { return "https://duckduckgo.com/?q=" + URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { throw new IllegalArgumentException("Cannot search for this text"); }
    }
    public static boolean canRemember(String url) {
        try {
            AgentPolicy.browserUrl(url); URI u = URI.create(url);
            String p = u.getPath() == null ? "" : u.getPath();
            String q = u.getQuery() == null ? "" : u.getQuery();
            return !p.matches("(?i).*[/](login|signin|sign-in|auth|oauth|checkout|payment|password|reset|access)(/.*|$)")
                    && !q.matches("(?i).*(^|&)(auth|code|token|flow_token|access_token|refresh_token|auth_token|password|secret|api_key|nonce|state)=.*");
        } catch (Exception e) { return false; }
    }
}

