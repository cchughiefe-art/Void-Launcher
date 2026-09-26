package com.voidlauncher.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PrivateGalleryActivity extends AppCompatActivity {
    private static final int IMPORT_FILES = 73;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private LinearLayout content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        PrivateAuth.authenticate(this, getSharedPreferences("void", MODE_PRIVATE), this::buildGallery);
    }

    private void buildGallery() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(28), dp(16), dp(16)); root.setBackgroundColor(0xff09090b);
        TextView title = text("Private Gallery", 28, Color.WHITE); root.addView(title);
        root.addView(text("Stored only inside Void Launcher", 14, 0xffaaaab5));
        LinearLayout actions = new LinearLayout(this);
        Button importButton = new Button(this); importButton.setText("Import files"); importButton.setOnClickListener(v -> pickFiles());
        Button refresh = new Button(this); refresh.setText("Refresh"); refresh.setOnClickListener(v -> refreshFiles());
        actions.addView(importButton, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(refresh, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(actions);
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
        refreshFiles();
    }

    private void pickFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, IMPORT_FILES);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != IMPORT_FILES || result != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        if (data.getData() != null) uris.add(data.getData());
        if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        IO.execute(() -> {
            File directory = new File(getFilesDir(), "private_gallery"); directory.mkdirs(); int saved = 0;
            for (Uri uri : uris) try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) continue;
                File target = uniqueFile(directory, displayName(uri));
                try (FileOutputStream output = new FileOutputStream(target)) {
                    byte[] buffer = new byte[32 * 1024]; int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
                saved++;
            } catch (Exception ignored) {}
            int total = saved; runOnUiThread(() -> { Toast.makeText(this, total + " file(s) imported privately", Toast.LENGTH_SHORT).show(); refreshFiles(); });
        });
    }

    private void refreshFiles() {
        if (content == null) return; content.removeAllViews();
        List<File> files = new ArrayList<>(); collect(files, new File(getFilesDir(), "private_gallery"));
        collect(files, new File(getFilesDir(), "private_downloads"));
        files.sort(Comparator.comparingLong(File::lastModified).reversed());
        if (files.isEmpty()) { content.addView(text("No private files yet. Import a file or download one in Private Browser.", 16, 0xffaaaab5)); return; }
        for (File file : files) addFile(file);
    }

    private void addFile(File file) {
        LinearLayout row = new LinearLayout(this); row.setGravity(android.view.Gravity.CENTER_VERTICAL); row.setPadding(0, dp(8), 0, dp(8));
        if (mime(file).startsWith("image/")) {
            ImageView preview = new ImageView(this); preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath())); row.addView(preview, new LinearLayout.LayoutParams(dp(72), dp(72)));
        }
        TextView label = text(file.getName() + "\n" + readableSize(file.length()), 15, Color.WHITE); label.setPadding(dp(12), 0, dp(8), 0);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        Button open = new Button(this); open.setText("Open"); open.setOnClickListener(v -> openFile(file)); row.addView(open);
        row.setOnLongClickListener(v -> { confirmDelete(file); return true; }); content.addView(row);
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".privatefiles", file);
            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime(file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception error) { Toast.makeText(this, "No app can open this file", Toast.LENGTH_SHORT).show(); }
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this).setTitle("Delete private file?").setMessage(file.getName())
            .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, w) -> { file.delete(); refreshFiles(); }).show();
    }

    private void collect(List<File> output, File directory) { File[] files = directory.listFiles(); if (files != null) output.addAll(Arrays.asList(files)); }
    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0).replaceAll("[^a-zA-Z0-9._ -]", "_");
        } catch (Exception ignored) {}
        return "private-file-" + System.currentTimeMillis();
    }
    private File uniqueFile(File directory, String name) {
        File file = new File(directory, name); int n = 1; int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name, ext = dot > 0 ? name.substring(dot) : "";
        while (file.exists()) file = new File(directory, base + " (" + n++ + ")" + ext); return file;
    }
    private String mime(File file) { String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName()); String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase()); return type == null ? "application/octet-stream" : type; }
    private String readableSize(long bytes) { if (bytes >= 1_048_576) return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1_048_576f); if (bytes >= 1024) return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024f); return bytes + " B"; }
    private TextView text(String value, int size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); return view; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
}
