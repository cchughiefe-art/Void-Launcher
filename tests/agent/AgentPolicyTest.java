package com.android.launcher3.agent;

public final class AgentPolicyTest {
    private static void reject(String url) {
        try { AgentPolicy.publicUrl(url); throw new AssertionError("Allowed unsupported URL"); }
        catch (IllegalArgumentException expected) {}
    }
    public static void main(String[] args) {
        if (!AgentPolicy.publicUrl("https://x.com/TeaonaX/status/123").startsWith("https://x.com/")) throw new AssertionError();
        for (String url : new String[]{"javascript:alert(1)", "http://x.com", "https://localhost/", "https://127.0.0.1/", "https://[::1]/", "https://host.local/", "https://user:pass@x.com/", "https://x.com/file.apk", "https://x.com/?api_key=example", "https://x.com/?%70assword=example"}) reject(url);
        if (!AgentPolicy.relayUrl("https://test-relay.trycloudflare.com/").equals("https://test-relay.trycloudflare.com")) throw new AssertionError();
        for (String url : new String[]{"https://x.com/", "https://test-relay.trycloudflare.com/path", "https://test-relay.trycloudflare.com/?key=value"}) {
            try { AgentPolicy.relayUrl(url); throw new AssertionError("Allowed invalid discovery endpoint"); } catch (IllegalArgumentException expected) {}
        }
        System.out.println("Agent policy checks passed");
    }
}
