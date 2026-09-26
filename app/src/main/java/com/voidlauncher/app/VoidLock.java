package com.voidlauncher.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

final class VoidLock {
    enum Profile { PRIVATE, DECOY }
    private final SharedPreferences prefs;
    VoidLock(Context context) { prefs = context.getSharedPreferences("void_lock", Context.MODE_PRIVATE); }
    boolean isConfigured() { return prefs.contains("private_hash") && prefs.contains("decoy_hash"); }
    void configure(String privatePin, String decoyPin) {
        if (privatePin.equals(decoyPin)) throw new IllegalArgumentException("PINs must be different");
        byte[] privateSalt = salt(), decoySalt = salt();
        prefs.edit().putString("private_salt", encode(privateSalt)).putString("private_hash", hash(privatePin, privateSalt))
            .putString("decoy_salt", encode(decoySalt)).putString("decoy_hash", hash(decoyPin, decoySalt)).apply();
    }
    Profile authenticate(String pin) {
        if (matches(pin, "private")) return Profile.PRIVATE;
        if (matches(pin, "decoy")) return Profile.DECOY;
        return null;
    }
    private boolean matches(String pin, String type) {
        String salt = prefs.getString(type + "_salt", ""), expected = prefs.getString(type + "_hash", "");
        if (salt.isEmpty() || expected.isEmpty()) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), hash(pin, Base64.decode(salt, Base64.NO_WRAP)).getBytes(StandardCharsets.UTF_8));
    }
    private static byte[] salt() { byte[] value = new byte[24]; new SecureRandom().nextBytes(value); return value; }
    private static String hash(String pin, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] bytes = pin.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < 120000; i++) digest.update(bytes);
            return encode(digest.digest());
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    private static String encode(byte[] value) { return Base64.encodeToString(value, Base64.NO_WRAP); }
}
