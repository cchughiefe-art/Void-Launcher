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
import java.util.HashMap;
import java.util.Map;
import android.view.View;
import android.webkit.ValueCallback;
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
    private final Map<Integer, WebView> pages = new HashMap<>();
    private final java.util.Set<Integer> failedPages = new java.util.HashSet<>();
    private BrowserTabs tabs;
    interface BrowserUi {
        void changed();
        boolean chooseFiles(WebView page, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params);
        void download(String url);
    }
    private BrowserUi ui;
    void setUi(BrowserUi ui) { this.ui = ui; }
    BrowserTabs tabs() { return tabs; }
    private CompletableFuture<JSONObject> pending;
    private JSONObject command;
    private long deadline;
    private String navigationError;
    private int generation;
    private boolean dead, navigationStarted, capturing;
    private Runnable inspect;
    AgentWebEngine(Context c, AgentSettings settings) {
        this.settings = settings; context = new MutableContextWrapper(c);
        tabs = new BrowserTabs(settings.prefs.getString("last_url", "https://x.com"));
        try {
            JSONArray saved = new JSONArray(settings.prefs.getString("browser_tabs", "[]"));
            for (int i=0; i<saved.length(); i++) {
                JSONObject t = saved.getJSONObject(i); String url = t.optString("url", "about:blank");
                if (!url.equals("about:blank")) url = AgentPolicy.browserUrl(url);
                if (i==0) tabs.get(BrowserTabs.AGENT).url = url; else tabs.add(url);
            }
            int selected = settings.prefs.getInt("browser_selected", BrowserTabs.AGENT);
            try { tabs.select(selected); } catch (Exception ignored) { tabs.select(BrowserTabs.AGENT); }
        } catch (Exception ignored) { tabs.select(BrowserTabs.AGENT); }
        web = createPage(BrowserTabs.AGENT);
    }
    private WebView createPage(int id) {
        AgentSettings.prepareWeb();
        final WebView page = new WebView(context); pages.put(id, page); page.setBackgroundColor(Color.rgb(10, 10, 14));
        WebSettings s = page.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false); s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(true); s.setGeolocationEnabled(false); s.setSaveFormData(false);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(page, false);
        page.setDownloadListener((url, ua, disposition, mime, size) -> {
            if (ui != null && tabs.selected() == id) ui.download(url);
            else settings.status("Open the browser to confirm a download.");
        });
        page.setWebChromeClient(new WebChromeClient() {
            @Override public void onReceivedTitle(WebView v, String title) { if (pages.get(id) == v) { tabs.get(id).title = title; changed(); } }
            @Override public void onProgressChanged(WebView v, int value) { changed(); }
            @Override public void onPermissionRequest(PermissionRequest r) { r.deny(); }
            @Override public boolean onShowFileChooser(WebView w, android.webkit.ValueCallback<Uri[]> cb, FileChooserParams p) { if (ui != null && tabs.selected() == id) return ui.chooseFiles(w, cb, p); cb.onReceiveValue(null); return true; }
        });
        page.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                try { AgentPolicy.browserUrl(r.getUrl().toString()); return false; }
                catch (Exception e) { settings.status("Blocked unsupported navigation."); return true; }
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String scheme = r.getUrl().getScheme();
                if ("data".equals(scheme) || "blob".equals(scheme)) return null;
                try { AgentPolicy.browserUrl(r.getUrl().toString()); return null; }
                catch (Exception e) { return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0])); }
            }
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) { if (v == web) navigationStarted = true; changed(); }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler handler, SslError e) {
                handler.cancel(); if (v == web) navigationError = "TLS validation failed; navigation stopped.";
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, android.webkit.WebResourceError e) {
                if (v == web && r.isForMainFrame()) navigationError = "Page could not load (" + e.getErrorCode() + ").";
            }
            @Override public void onPageFinished(WebView v, String url) {
                if (pages.get(id) != v) return;
                BrowserTabs.Tab t = tabs.get(id); t.url = url == null ? "about:blank" : url;
                t.title = v.getTitle() == null ? "Page" : v.getTitle();
                if (v == web && BrowserAddress.canRemember(t.url)) settings.prefs.edit().putString("last_url", t.url).apply();
                remember(t); saveTabs(); changed();
                CookieManager.getInstance().flush();
            }
            @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail detail) {
                if (v == web) { dead = true; fail("Agent tab renderer stopped; open the Agent tab to recover."); }
                failedPages.add(id); pages.remove(id);
                if (v.getParent() instanceof ViewGroup) ((ViewGroup)v.getParent()).removeView(v);
                v.destroy(); settings.status("A tab renderer stopped; select that tab to recover."); changed(); return true;
            }
        });
        page.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(1700, android.view.View.MeasureSpec.EXACTLY));
        page.layout(0, 0, 1080, 1700);
        String initial = tabs.get(id).url;
        if (initial.equals("about:blank")) startPage(page); else page.loadUrl(initial);
        return page;
    }
    private void changed() { if (ui != null) ui.changed(); }
    private WebView selectedPage() {
        int id = tabs.selected(); WebView page = pages.get(id);
        if (page == null || failedPages.contains(id)) { failedPages.remove(id); page = createPage(id); if (web == null || dead) { web = page; dead = false; } }
        return page;
    }
    WebView view(Context activity) {
        context.setBaseContext(activity); WebView page = selectedPage();
        if (page.getParent() instanceof ViewGroup) ((ViewGroup)page.getParent()).removeView(page);
        page.onResume(); return page;
    }
    void detach(Context application) {
        for (WebView page : pages.values()) if (page.getParent() instanceof ViewGroup) ((ViewGroup)page.getParent()).removeView(page);
        context.setBaseContext(application); CookieManager.getInstance().flush();
    }
    String activeUrl() { WebView page = selectedPage(); return page.getUrl() == null ? "about:blank" : page.getUrl(); }
    String activeTitle() { return selectedPage().getTitle(); }
    int progress() { return selectedPage().getProgress(); }
    boolean canBack() { return selectedPage().canGoBack(); }
    boolean canForward() { return selectedPage().canGoForward(); }
    void back() { checkManual(); if (canBack()) selectedPage().goBack(); }
    void forward() { checkManual(); if (canForward()) selectedPage().goForward(); }
    void reload() { checkManual(); selectedPage().reload(); }
    void find(String term) { selectedPage().findAllAsync(term); }
    void findNext(boolean next) { selectedPage().findNext(next); }
    void clearFind() { selectedPage().clearMatches(); }
    private void checkManual() { if (pending != null && pages.get(tabs.selected()) == web) throw new IllegalStateException("This tab is being read by the agent. Other tabs remain available."); }
    void navigateLocal(String url) {
        checkManual(); WebView page = selectedPage();
        if (url.equals("about:blank")) startPage(page); else page.loadUrl(AgentPolicy.browserUrl(url));
    }
    int newTab(String url) { int id = tabs.add(url); saveTabs(); return id; }
    void selectTab(int id) { tabs.select(id); saveTabs(); }
    boolean closeTab(int id) {
        if (pending != null && pages.get(id) == web) throw new IllegalStateException("Stop agent access before closing the tab it is reading.");
        if (!tabs.close(id)) return false;
        WebView page = pages.remove(id); if (page != null) { if (page.getParent() instanceof ViewGroup) ((ViewGroup)page.getParent()).removeView(page); page.destroy(); }
        failedPages.remove(id); if (page == web) { web = selectedPage(); dead = false; } saveTabs(); return true;
    }
    private void startPage(WebView page) {
        page.loadDataWithBaseURL(null, "<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='background:#101827;color:#e8edf5;font:18px sans-serif;padding:28px'><h1>VOID</h1><p>Search or enter an address above.</p><p><a style='color:#97bcff' href='https://x.com'>X</a> &nbsp; <a style='color:#97bcff' href='https://github.com'>GitHub</a> &nbsp; <a style='color:#97bcff' href='https://duckduckgo.com'>Search</a></p></body>", "text/html", "UTF-8", null);
    }
    private void saveTabs() {
        JSONArray out = new JSONArray();
        for (BrowserTabs.Tab t : tabs.all()) try { out.put(new JSONObject().put("url", BrowserAddress.canRemember(t.url) ? t.url : "about:blank")); } catch (Exception ignored) {}
        // Restore selection by its list position, since tab IDs are session-local.
        int position=1; for(int i=0;i<tabs.all().size();i++) if(tabs.all().get(i).id==tabs.selected()) position=i+1;
        settings.prefs.edit().putString("browser_tabs", out.toString()).putInt("browser_selected", position).apply();
    }
    private void remember(BrowserTabs.Tab tab) {
        if (!BrowserAddress.canRemember(tab.url)) return;
        try {
            JSONArray old = new JSONArray(settings.prefs.getString("browser_history", "[]")); JSONArray out = new JSONArray();
            out.put(new JSONObject().put("url", tab.url).put("title", tab.title));
            for(int i=0;i<old.length()&&out.length()<100;i++) if(!old.getJSONObject(i).optString("url").equals(tab.url)) out.put(old.get(i));
            settings.prefs.edit().putString("browser_history",out.toString()).apply();
        } catch (Exception ignored) {}
    }
    CompletableFuture<JSONObject> execute(JSONObject c) {
        CompletableFuture<JSONObject> f = new CompletableFuture<>();
        main.post(() -> {
            if (!settings.enabled()) { f.complete(error("Connection stopped.")); return; }
            if (pending != null) { f.complete(error("Another browser command is running.")); return; }
            try {
                String action = c.getString("action");
                if (action.equals("tabs")) { f.complete(tabInventory()); return; }
                if (action.equals("new_tab")) { String url = c.optString("url", "about:blank"); if (!url.equals("about:blank")) AgentPolicy.publicUrl(url); newTab(url); selectedPage(); changed(); f.complete(tabInventory()); return; }
                int targetId = c.has("tabId") ? c.getInt("tabId") : tabs.selected(); tabs.get(targetId);
                if (action.equals("select_tab")) { selectTab(targetId); changed(); f.complete(tabInventory()); return; }
                if (action.equals("close_tab")) { closeTab(targetId); changed(); f.complete(tabInventory()); return; }
                if (!action.equals("open") && !action.equals("capture") && !action.equals("scroll") && !action.equals("click_link") && !action.equals("fill") && !action.equals("click") && !action.equals("submit")) throw new IllegalArgumentException("Unsupported action");
                if (failedPages.contains(targetId)) throw new IllegalStateException("Tab renderer stopped. Select that tab to recover.");
                web = pages.get(targetId); if (web == null) web = createPage(targetId); dead = false;
                if (action.equals("open") || action.equals("click_link")) AgentPolicy.publicUrl(c.getString("url"));
                if (action.equals("click_link")) AgentPolicy.publicUrl(c.getString("pageUrl"));
                if (action.equals("scroll") && !c.optString("direction").matches("up|down")) throw new IllegalArgumentException("Invalid scroll direction");
                pending = f; command = c; navigationError = null; capturing = false; deadline = android.os.SystemClock.elapsedRealtime() + 20000;
                int epoch = ++generation;
                eval("JSON.stringify({blocked:/verify you are human|checking your browser|unusual activity|automated requests|account suspended|temporarily restricted|rate limit exceeded|too many requests/i.test(document.body?.innerText||'')})", guard -> {
                    if (!current(epoch)) return;
                    if (guard.optBoolean("blocked")) { blocked(); return; }
                    if (action.equals("fill") || action.equals("click") || action.equals("submit")) {
                        if (!settings.prefs.getBoolean("page_actions", false)) { fail("Enable forms and posting in browser Settings on your phone."); return; }
                        AgentPolicy.publicUrl(c.optString("pageUrl"));
                        eval(AgentPageControls.script(c.toString()), result -> { if(current(epoch)) { if(result.optBoolean("blocked"))blocked();else finish(result); } });
                    } else dispatch(epoch);
                });
                main.postDelayed(() -> { if (current(epoch)) fail("Page read timed out; not retried. Bring Agent Browser to the foreground if Android paused it."); }, 21000);
            } catch (Exception e) { if (pending == f) fail("Invalid browser command."); else f.complete(error("Invalid browser command.")); }
        }); return f;
    }
    private JSONObject tabInventory() {
        JSONObject out = new JSONObject(); JSONArray list = new JSONArray();
        for (BrowserTabs.Tab t : tabs.all()) try {
            list.put(new JSONObject().put("id",t.id).put("title",t.title == null ? "Page" : t.title)
                .put("url",BrowserAddress.canRemember(t.url) ? t.url : "[sign-in or transient page]"));
        } catch (Exception ignored) {}
        try { out.put("tabs",list).put("selectedTabId",tabs.selected()); } catch (Exception ignored) {} return out;
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
        if (android.os.SystemClock.elapsedRealtime() >= deadline) { fail("Page did not become readable before timeout."); return; }
        inspect = () -> waitForPage(epoch, navigation); main.postDelayed(inspect, 350);
        eval(js, r -> {
            if (!current(epoch) || capturing) return;
            boolean moved = !navigation || (navigationStarted && (requested.equals(r.optString("url")) || (web.getProgress() == 100 && !"about:blank".equals(r.optString("url")) && !r.optString("url").isEmpty())));
            if (moved && !"loading".equals(r.optString("ready")) && r.optInt("text") > 30 && web.getProgress() >= 80) { capture(epoch); return; }
            if (android.os.SystemClock.elapsedRealtime() >= deadline) { fail("Page did not become readable before timeout."); return; }
        });
    }
    private void capture(int epoch) {
        if(capturing)return;capturing=true;if(inspect!=null)main.removeCallbacks(inspect);
        try { AgentPolicy.publicUrl(web.getUrl()); } catch (Exception e) { fail("Page is not an allowed HTTPS page."); return; }
        eval(AgentCapture.SCRIPT, result -> {
            if (!current(epoch)) return;
            if (result.optBoolean("blocked")) { blocked(); return; }
            if (!result.has("error") && (!result.has("url") || !result.optString("url").equals(web.getUrl()))) { fail("Capture URL did not match the browser page."); return; }
            if (result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 170000) { fail("Capture too large; choose a narrower page."); return; }
            eval(AgentPageControls.script(inspectRequest()), controls -> {
                if(!current(epoch))return;
                if(controls.optBoolean("blocked")){blocked();return;}
                try{result.put("pageControls",controls);}catch(Exception ignored){} finish(result);
            });
        });
    }
    private String inspectRequest() { JSONObject request=new JSONObject(); try{request.put("action","inspect");}catch(Exception ignored){}return request.toString(); }
    private void blocked() { JSONObject e = error("Site restriction encountered; connection stopped."); try { e.put("blocked", true); } catch (Exception ignored) {} finish(e); }
    private void eval(String script, java.util.function.Consumer<JSONObject> callback) {
        web.evaluateJavascript(script, raw -> {
            try { Object v = new JSONTokener(raw).nextValue(); callback.accept(v instanceof String ? new JSONObject((String) v) : new JSONObject()); }
            catch (Exception e) { fail("Browser returned an unreadable result."); }
        });
    }
    private void finish(JSONObject result) { try { result.put("tabId", command != null && command.has("tabId") ? command.getInt("tabId") : findTabId(web)); result.put("tabs",tabInventory().getJSONArray("tabs")); result.put("selectedTabId",tabs.selected()); } catch(Exception ignored) {} if(result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>170000) result=error("Capture too large; select a narrower page."); CompletableFuture<JSONObject> f = pending; pending = null; command = null; capturing = false; if (inspect != null) main.removeCallbacks(inspect); if (f != null) f.complete(result); }
    private int findTabId(WebView page) { for(Map.Entry<Integer,WebView> entry:pages.entrySet()) if(entry.getValue()==page)return entry.getKey(); return -1; }
    private void fail(String message) { finish(error(message)); }
    static JSONObject error(String message) { JSONObject j = new JSONObject(); try { j.put("error", message).put("at", System.currentTimeMillis()); } catch (Exception ignored) {} return j; }
    void stopCommands() { generation++; if (!dead && web != null) web.stopLoading(); fail("Connection stopped by user."); }
    void destroy() { stopCommands(); ui = null; detach(context.getApplicationContext()); for (WebView page : pages.values()) page.destroy(); pages.clear(); }
}

