package com.android.launcher3.agent;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Never forwards device credentials on redirects; no cookies or X credentials are sent. */
final class AgentRelay {
    static final String DISCOVERY = "https://raw.githubusercontent.com/cchughiefe-art/void-xmd/main/bridge-discovery.json";
    private final AgentSettings settings;
    private String endpoint = "";
    AgentRelay(AgentSettings settings) { this.settings = settings; }
    synchronized String discover() throws Exception {
        JSONObject d = request(DISCOVERY + "?t=" + System.currentTimeMillis(), null, null);
        endpoint = AgentPolicy.relayUrl(d.getString("endpoint"));
        settings.prefs.edit().putString("endpoint", endpoint).apply(); return endpoint;
    }
    JSONObject call(String path, JSONObject data) throws Exception {
        String root; synchronized (this) { root = endpoint; }
        if (root.isEmpty()) throw new IllegalStateException("Relay not discovered");
        return request(root + path, settings.deviceKey(), data);
    }
    private static JSONObject request(String url, String token, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(8000); c.setReadTimeout(10000); c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Accept", "application/json"); c.setRequestProperty("Cache-Control", "no-store");
            if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) { c.setRequestMethod("POST"); c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json");
                try (java.io.OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); } }
            int code = c.getResponseCode();
            if (code != 200 && code != 201) throw new RelayException(code);
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) { if (out.size() + n > 210000) throw new IllegalStateException("Relay response too large"); out.write(b, 0, n); }
                return new JSONObject(out.toString("UTF-8"));
            }
        } finally { c.disconnect(); }
    }
    static final class RelayException extends Exception {
        final int code;
        RelayException(int code) { super("Relay HTTP " + code); this.code = code; }
    }
}
