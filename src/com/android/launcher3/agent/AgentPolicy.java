package com.android.launcher3.agent;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Shared by local navigation and remote read commands. No secret-bearing URLs. */
public final class AgentPolicy {
    private AgentPolicy() {}
    public static String publicUrl(String value) { return validate(value, false); }
    public static String browserUrl(String value) { return validate(value, true); }
    private static String validate(String value, boolean manual) {
        try {
            URI u = new URI(value);
            String h = u.getHost();
            if (h == null) throw new IllegalArgumentException();
            h = h.toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getRawUserInfo() != null
                    || (u.getPort() != -1 && u.getPort() != 443) || !h.contains(".")
                    || h.endsWith(".local") || h.endsWith(".internal") || h.endsWith(".localhost")
                    || h.matches("[0-9.]+") || h.contains(":")) throw new IllegalArgumentException();
            String path = u.getPath() == null ? "" : u.getPath();
            if (!manual && path.toLowerCase(Locale.ROOT).matches(".*\\.(exe|apk|crx|dmg|msi|zip)$")) throw new IllegalArgumentException();
            String query = u.getQuery();
            if (query != null && Pattern.compile("(^|&)(access_token|auth_token|api_key|secret|password)=", Pattern.CASE_INSENSITIVE).matcher(query).find()) throw new IllegalArgumentException();
            return u.toASCIIString();
        } catch (Exception e) { throw new IllegalArgumentException("Use a public HTTPS address without credentials or downloads."); }
    }
    public static String relayUrl(String value) {
        String safe = publicUrl(value);
        URI u = URI.create(safe);
        if (!u.getHost().matches("[a-z0-9-]+\\.trycloudflare\\.com")
                || u.getRawQuery() != null || u.getRawFragment() != null
                || !(u.getPath().isEmpty() || "/".equals(u.getPath()))) {
            throw new IllegalArgumentException("Invalid relay discovery address.");
        }
        return "https://" + u.getHost();
    }
}

