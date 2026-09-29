package com.android.launcher3;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** AES-256-GCM encrypted storage backed by an Android Keystore key. */
public final class VoidVaultCrypto {
    private static final String ALIAS = "void_launcher_private_vault_v1";
    private static final String PREFS = "void_vault_index";
    private static final String SEP = "\u001f";
    private static final int VERSION = 1;
    private final Context context;
    private final File directory;
    private final SharedPreferences index;

    public static final class Entry {
        public final String id;
        public final String name;
        public final String mime;
        public final long createdAt;
        Entry(String id, String name, String mime, long createdAt) {
            this.id = id; this.name = name; this.mime = mime; this.createdAt = createdAt;
        }
    }

    public VoidVaultCrypto(Context context) {
        this.context = context.getApplicationContext();
        directory = new File(this.context.getFilesDir(), "void_vault");
        if (!directory.exists()) directory.mkdirs();
        index = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    public Entry store(byte[] plain, String displayName, String mime) throws Exception {
        String id = UUID.randomUUID().toString();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(plain);
        try (FileOutputStream out = new FileOutputStream(new File(directory, id + ".vlt"))) {
            out.write(VERSION);
            out.write(cipher.getIV().length);
            out.write(cipher.getIV());
            out.write(encrypted);
        }
        long now = System.currentTimeMillis();
        String safeName = displayName == null || displayName.trim().isEmpty() ? "Private file" : displayName;
        String safeMime = mime == null ? "application/octet-stream" : mime;
        String meta = encode(safeName) + SEP + encode(safeMime) + SEP + now;
        if (!index.edit().putString(id, meta).commit()) {
            new File(directory, id + ".vlt").delete();
            throw new IllegalStateException("Could not update encrypted vault index");
        }
        return new Entry(id, safeName, safeMime, now);
    }

    public byte[] read(String id) throws Exception {
        File file = new File(directory, id + ".vlt");
        try (FileInputStream in = new FileInputStream(file); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            int version = in.read();
            int ivLength = in.read();
            if (version != VERSION || ivLength < 12 || ivLength > 32) throw new SecurityException("Invalid vault file");
            byte[] iv = new byte[ivLength];
            if (in.read(iv) != ivLength) throw new SecurityException("Truncated vault file");
            byte[] buffer = new byte[16384];
            int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return cipher.doFinal(bytes.toByteArray());
        }
    }

    public List<Entry> list() {
        List<Entry> entries = new ArrayList<>();
        for (String id : index.getAll().keySet()) {
            String value = index.getString(id, "");
            String[] parts = value.split(SEP, -1);
            if (parts.length != 3 || !new File(directory, id + ".vlt").isFile()) continue;
            try { entries.add(new Entry(id, decode(parts[0]), decode(parts[1]), Long.parseLong(parts[2]))); }
            catch (RuntimeException ignored) { }
        }
        Collections.sort(entries, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return entries;
    }

    public boolean delete(String id) {
        boolean removed = new File(directory, id + ".vlt").delete();
        index.edit().remove(id).apply();
        return removed;
    }

    private static String encode(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }
    private static String decode(String value) {
        return new String(Base64.decode(value, Base64.NO_WRAP), StandardCharsets.UTF_8);
    }
}
