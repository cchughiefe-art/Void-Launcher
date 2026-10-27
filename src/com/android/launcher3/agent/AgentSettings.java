package com.android.launcher3.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.WebView;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class AgentSettings {
    private static boolean webReady;
    private static final String ALIAS = "void_agent_device_key_v1";
    final SharedPreferences prefs;
    AgentSettings(Context c) { prefs = c.getSharedPreferences("void_agent_browser", Context.MODE_PRIVATE); }
    // Called only in :void_agent, before any WebView/CookieManager API.
    static synchronized void prepareWeb() {
        if (!webReady) {
            if (Build.VERSION.SDK_INT >= 28) WebView.setDataDirectorySuffix("void_agent");
            webReady = true;
        }
    }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return gen.generateKey();
    }
    void saveDeviceKey(String value) throws Exception {
        if (value.length() < 32 || value.length() > 512 || value.contains("\n") || value.contains("\r")) throw new IllegalArgumentException("Enter the device key from your server.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        prefs.edit().putString("key_iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString("key_enc", Base64.encodeToString(cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)), Base64.NO_WRAP)).commit();
    }
    String deviceKey() throws Exception {
        String encrypted = prefs.getString("key_enc", ""); if (encrypted.isEmpty()) return "";
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("key_iv", ""), Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), java.nio.charset.StandardCharsets.UTF_8);
    }
    boolean enabled() { return prefs.getBoolean("enabled", false); }
    void enabled(boolean v) { prefs.edit().putBoolean("enabled", v).commit(); }
    void status(String text) { prefs.edit().putString("status", text).putLong("status_at", System.currentTimeMillis()).apply(); }
}
