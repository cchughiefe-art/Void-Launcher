package com.android.launcher3.agent;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.RenderProcessGoneDetail;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;

/** Service-owned WebView; attachment to a screen is optional and never owns its lifetime. */
final class AgentWebEngine {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableContextWrapper context;
    private final AgentSettings settings;
    private WebView web;
    private CompletableFuture<JSONObject> pending;
    private JSONObject command;
    private long deadline;
    private String navigationError;
    private int generation;
    private boolean dead, navigationStarted;
    private Runnable inspect;
    AgentWebEngine(Context c, AgentSettings settings) {
        this.settings = settings; context = new MutableContextWrapper(c); create();
    }
    private void create() {
        AgentSettings.prepareWeb();
        web = new WebView(context); web.setBackgroundColor(Color.rgb(10, 10, 14));
        WebSettings s = web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false); s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(true); s.setGeolocationEnabled(false); s.setSaveFormData(false);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setDownloadListener((url, ua, disposition, mime, size) -> settings.status("Downloads are disabled in Agent Browser."));
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest r) { r.deny(); }
            @Override public boolean onShowFileChooser(WebView w, android.webkit.ValueCallback<Uri[]> cb, FileChooserParams p) { cb.onReceiveValue(null); return true; }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                try { AgentPolicy.publicUrl(r.getUrl().toString()); return false; }
                catch (Exception e) { settings.status("Blocked unsupported navigation."); return true; }
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String scheme = r.getUrl().getScheme();
                if ("data".equals(scheme) || "blob".equals(scheme)) return null;
                try { AgentPolicy.publicUrl(r.getUrl().toString()); return null; }
                catch (Exception e) { return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0])); }
            }
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) { navigationStarted = true; }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler handler, SslError e) {
                handler.cancel(); navigationError = "TLS validation failed; navigation stopped.";
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, android.webkit.WebResourceError e) {
                if (r.isForMainFrame()) navigationError = "Page could not load (" + e.getErrorCode() + ").";
            }
            @Override public void onPageFinished(WebView v, String url) {
                if (url != null && url.startsWith("https://")) settings.prefs.edit().putString("last_url", url).apply();
                CookieManager.getInstance().flush();
            }
            @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail detail) {
                dead = true; fail("Browser renderer stopped. Open Agent Browser to recover.");
                if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
                web.destroy(); settings.status("Browser renderer stopped; waiting for recovery."); return true;
            }
        });
        web.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(1700, android.view.View.MeasureSpec.EXACTLY));
        web.layout(0, 0, 1080, 1700);
        web.loadUrl("about:blank");
    }
    WebView view(Context activity) {
        if (dead) { dead = false; create(); }
        context.setBaseContext(activity);
        if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
        web.onResume(); return web;
    }
    void detach(Context application) {
        if (web.getParent() instanceof ViewGroup) ((ViewGroup) web.getParent()).removeView(web);
        context.setBaseContext(application); CookieManager.getInstance().flush();
    }
    void navigateLocal(String url) {
        if (pending != null) throw new IllegalStateException("Wait for the current read to finish.");
        web.loadUrl(AgentPolicy.publicUrl(url));
    }
    CompletableFuture<JSONObject> execute(JSONObject c) {
        CompletableFuture<JSONObject> f = new CompletableFuture<>();
        main.post(() -> {
            if (!settings.enabled()) { f.complete(error("Connection stopped.")); return; }
            if (dead) { f.complete(error("Browser renderer unavailable. Open Agent Browser.")); return; }
            if (pending != null) { f.complete(error("Another browser command is running.")); return; }
            try {
                String action = c.getString("action");
                if (!action.equals("open") && !action.equals("capture") && !action.equals("scroll") && !action.equals("click_link")) throw new IllegalArgumentException("Unsupported action");
                if (action.equals("open") || action.equals("click_link")) AgentPolicy.publicUrl(c.getString("url"));
                if (action.equals("click_link")) AgentPolicy.publicUrl(c.getString("pageUrl"));
                if (action.equals("scroll") && !c.optString("direction").matches("up|down")) throw new IllegalArgumentException("Invalid scroll direction");
                pending = f; command = c; navigationError = null; deadline = android.os.SystemClock.elapsedRealtime() + 20000;
                int epoch = ++generation;
                eval("JSON.stringify({blocked:/verify you are human|checking your browser|unusual activity|automated requests|account suspended|temporarily restricted|rate limit exceeded|too many requests/i.test(document.body?.innerText||'')})", guard -> {
                    if (!current(epoch)) return;
                    if (guard.optBoolean("blocked")) { blocked(); return; }
                    dispatch(epoch);
                });
                main.postDelayed(() -> { if (current(epoch)) fail("Page read timed out; not retried. Bring Agent Browser to the foreground if Android paused it."); }, 21000);
            } catch (Exception e) { if (pending == f) fail("Invalid browser command."); else f.complete(error("Invalid browser command.")); }
        }); return f;
    }
    private boolean current(int epoch) { return generation == epoch && pending != null && settings.enabled(); }
    private void dispatch(int epoch) {
        String action = command.optString("action");
        if (action.equals("open")) { navigationStarted = false; web.loadUrl(command.optString("url")); waitForPage(epoch, true); }
        else if (action.equals("click_link")) {
            String js = "JSON.stringify({valid:location.href===" + JSONObject.quote(command.optString("pageUrl")) + "&&[...document.querySelectorAll('a[href]')].some(a=>a.href===" + JSONObject.quote(command.optString("url")) + "&&(a.innerText||a.getAttribute('aria-label')||'').trim().slice(0,300)===" + JSONObject.quote(command.optString("text")) + "&&a.getClientRects().length&&!a.hasAttribute('download'))})";
            eval(js, result -> { if (!current(epoch)) return; if (!result.optBoolean("valid")) { fail("Source page or link changed. Capture again."); return; } navigationStarted = false; web.loadUrl(command.optString("url")); waitForPage(epoch, true); });
        } else if (action.equals("scroll")) {
            web.evaluateJavascript("window.scrollBy(0," + (command.optString("direction").equals("up") ? "-" : "") + "Math.min(innerHeight,900));", null);
            main.postDelayed(() -> { if (current(epoch)) waitForPage(epoch, false); }, 500);
        } else waitForPage(epoch, false);
    }
    private void waitForPage(int epoch, boolean navigation) {
        if (!current(epoch)) return;
        if (navigationError != null) { fail(navigationError); return; }
        // Wait for the requested URL rather than capturing the old page after a fixed sleep.
        String requested = command.optString("url");
        String js = "JSON.stringify({url:location.href,ready:document.readyState,text:(document.body?.innerText||'').trim().length})";
        eval(js, r -> {
            if (!current(epoch)) return;
            boolean moved = !navigation || (navigationStarted && (requested.equals(r.optString("url")) || (web.getProgress() == 100 && !"about:blank".equals(r.optString("url")) && !r.optString("url").isEmpty())));
            if (moved && !"loading".equals(r.optString("ready")) && r.optInt("text") > 30 && web.getProgress() >= 80) { capture(epoch); return; }
            if (android.os.SystemClock.elapsedRealtime() >= deadline) { fail("Page did not become readable before timeout."); return; }
            inspect = () -> waitForPage(epoch, navigation); main.postDelayed(inspect, 350);
        });
    }
    private void capture(int epoch) {
        try { AgentPolicy.publicUrl(web.getUrl()); } catch (Exception e) { fail("Page is not an allowed HTTPS page."); return; }
        eval(AgentCapture.SCRIPT, result -> {
            if (!current(epoch)) return;
            if (result.optBoolean("blocked")) { blocked(); return; }
            if (!result.has("error") && (!result.has("url") || !result.optString("url").equals(web.getUrl()))) { fail("Capture URL did not match the browser page."); return; }
            if (result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 170000) { fail("Capture too large; choose a narrower page."); return; }
            finish(result);
        });
    }
    private void blocked() { JSONObject e = error("Site restriction encountered; connection stopped."); try { e.put("blocked", true); } catch (Exception ignored) {} finish(e); }
    private void eval(String script, java.util.function.Consumer<JSONObject> callback) {
        web.evaluateJavascript(script, raw -> {
            try { Object v = new JSONTokener(raw).nextValue(); callback.accept(v instanceof String ? new JSONObject((String) v) : new JSONObject()); }
            catch (Exception e) { fail("Browser returned an unreadable result."); }
        });
    }
    private void finish(JSONObject result) { CompletableFuture<JSONObject> f = pending; pending = null; command = null; if (inspect != null) main.removeCallbacks(inspect); if (f != null) f.complete(result); }
    private void fail(String message) { finish(error(message)); }
    static JSONObject error(String message) { JSONObject j = new JSONObject(); try { j.put("error", message).put("at", System.currentTimeMillis()); } catch (Exception ignored) {} return j; }
    void stopCommands() { generation++; if (!dead) web.stopLoading(); fail("Connection stopped by user."); }
    void destroy() { stopCommands(); if (!dead) { detach(context.getApplicationContext()); web.destroy(); } }
}
