package com.android.launcher3.agent;

import android.webkit.WebView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;

/**
 * Opt-in WebView test geolocation and browser-only network routing.
 * Neither changes Android's physical GPS or establishes account residency.
 */
final class BrowserRegionSettings {
    private BrowserRegionSettings() {}

    static boolean geoEnabled(AgentSettings settings) {
        return settings.prefs.getBoolean("geo_override_enabled", false);
    }

    static double latitude(AgentSettings settings) {
        return coordinate(settings.prefs.getString("geo_latitude", "40.7128"), -90, 90);
    }

    static double longitude(AgentSettings settings) {
        return coordinate(settings.prefs.getString("geo_longitude", "-74.0060"), -180, 180);
    }

    static double coordinate(String value, double min, double max) {
        double d = Double.parseDouble(value.trim());
        if (!Double.isFinite(d) || d < min || d > max) throw new IllegalArgumentException("Coordinate outside valid range");
        return d;
    }

    static String proxyRule(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.length() > 255) throw new IllegalArgumentException("Proxy address too long");
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https") && !scheme.equals("socks")) {
                throw new IllegalArgumentException("Use http://, https:// or socks://");
            }
            if (uri.getHost() == null || uri.getHost().isEmpty() || uri.getPort() < 1 || uri.getPort() > 65535
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty())) {
                throw new IllegalArgumentException("Enter an unauthenticated proxy host and port, without a path");
            }
            return scheme + "://" + uri.getRawAuthority();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Invalid proxy address");
        }
    }

    static boolean installGeolocation(WebView page, AgentSettings settings) {
        if (!geoEnabled(settings)) return false;
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            settings.status("Document-start JavaScript unavailable: test GPS is not active on this WebView.");
            return false;
        }
        WebViewCompat.addDocumentStartJavaScript(page, script(latitude(settings), longitude(settings)),
                Collections.singleton("*"));
        return true;
    }

    static String script(double latitude, double longitude) {
        return "(function(){'use strict';"
                + "const c=Object.freeze({latitude:" + latitude + ",longitude:" + longitude
                + ",accuracy:25,altitude:null,altitudeAccuracy:null,heading:null,speed:null});"
                + "let nextId=1;const watches=new Map();"
                + "function position(){return {coords:c,timestamp:Date.now()};}"
                + "const geo=Object.freeze({"
                + "getCurrentPosition:function(ok){if(typeof ok==='function')setTimeout(()=>ok(position()),0);},"
                + "watchPosition:function(ok){const id=nextId++;if(typeof ok==='function'){watches.set(id,ok);setTimeout(()=>{if(watches.has(id))ok(position());},0);}return id;},"
                + "clearWatch:function(id){watches.delete(id);}});"
                + "try{Object.defineProperty(navigator,'geolocation',{configurable:true,get:function(){return geo;}});}"
                + "catch(e){try{Object.defineProperty(Navigator.prototype,'geolocation',{configurable:true,get:function(){return geo;}});}catch(ignored){}}"
                + "})();";
    }
}
