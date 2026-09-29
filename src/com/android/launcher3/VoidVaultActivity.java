package com.android.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Private gallery and document vault. Decrypted bytes never leave this activity. */
public class VoidVaultActivity extends Activity {
    private static final int PICK_FILE = 7731;
    private static final int BG = Color.rgb(10, 10, 14);
    private static final int ACCENT = Color.rgb(139, 92, 246);
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private VoidVaultCrypto vault;
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        vault = new VoidVaultCrypto(this);
        render();
    }

    private void render() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(16)); root.setBackgroundColor(BG);
        TextView title = text("Encrypted Vault", 30, Color.WHITE); root.addView(title);
        root.addView(text("Photos, documents and private-browser downloads are encrypted with a key held by Android Keystore.", 15, Color.LTGRAY));
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add = button("Import file"); add.setOnClickListener(v -> pickFile()); actions.addView(add, new LinearLayout.LayoutParams(0, dp(54), 1));
        Button browser = button("Private browser"); browser.setOnClickListener(v -> startActivity(new Intent(this, VoidPrivateBrowserActivity.class))); actions.addView(browser, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(actions);
        List<VoidVaultCrypto.Entry> entries = vault.list();
        if (entries.isEmpty()) {
            TextView empty = text("The vault is empty. Import a file or download one from the private browser.", 17, Color.GRAY);
            empty.setGravity(Gravity.CENTER); root.addView(empty, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            ListView list = new ListView(this); list.setDividerHeight(1); list.setBackgroundColor(BG);
            List<String> labels = new ArrayList<>();
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            for (VoidVaultCrypto.Entry entry : entries) labels.add(entry.name + "\n" + format.format(new Date(entry.createdAt)));
            list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, labels) {
                @Override public View getView(int p, View c, android.view.ViewGroup parent) {
                    TextView view = (TextView) super.getView(p, c, parent); view.setTextColor(Color.WHITE); view.setTextSize(16); view.setPadding(dp(12), dp(14), dp(12), dp(14)); return view;
                }
            });
            list.setOnItemClickListener((p, v, pos, id) -> open(entries.get(pos)));
            list.setOnItemLongClickListener((p, v, pos, id) -> { confirmDelete(entries.get(pos)); return true; });
            root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        setContentView(root);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_FILE || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData(); String name = displayName(uri); String mime = getContentResolver().getType(uri);
        Toast.makeText(this, "Encrypting " + name + "…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16384]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                vault.store(out.toByteArray(), name, mime);
                runOnUiThread(() -> { Toast.makeText(this, "Stored in encrypted vault", Toast.LENGTH_SHORT).show(); render(); });
            } catch (Exception error) { runOnUiThread(() -> Toast.makeText(this, "Import failed: " + error.getMessage(), Toast.LENGTH_LONG).show()); }
        });
    }

    private void open(VoidVaultCrypto.Entry entry) {
        io.execute(() -> {
            try {
                byte[] bytes = vault.read(entry.id);
                runOnUiThread(() -> {
                    if (entry.mime.startsWith("image/")) {
                        ImageView image = new ImageView(this); image.setAdjustViewBounds(true); image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                        new AlertDialog.Builder(this).setTitle(entry.name).setView(image).setPositiveButton("Close", null).show();
                    } else {
                        new AlertDialog.Builder(this).setTitle(entry.name).setMessage("Encrypted " + entry.mime + "\n\nThis version keeps non-image documents sealed inside the vault.").setPositiveButton("Close", null).show();
                    }
                });
            } catch (Exception error) { runOnUiThread(() -> Toast.makeText(this, "Could not decrypt file", Toast.LENGTH_LONG).show()); }
        });
    }

    private void confirmDelete(VoidVaultCrypto.Entry entry) {
        new AlertDialog.Builder(this).setTitle("Delete encrypted file?").setMessage(entry.name)
                .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, w) -> { vault.delete(entry.id); render(); }).show();
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Exception ignored) { }
        return "Private file";
    }

    private TextView text(String value, int size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setPadding(0, dp(8), 0, dp(8)); return v; }
    private Button button(String value) { Button v = new Button(this); v.setText(value); v.setTextColor(Color.WHITE); v.setBackgroundColor(ACCENT); return v; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
