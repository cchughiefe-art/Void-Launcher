package com.voidlauncher.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.appcompat.content.res.AppCompatResources;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;
import java.io.File;
import java.io.FileOutputStream;

final class WallpaperDepthEngine {
    interface Callback { void complete(boolean success, String message); }
    private WallpaperDepthEngine() {}

    static void generate(Context context, Uri uri, String space, Callback callback) {
        try { process(context, InputImage.fromFilePath(context, uri), space, callback); }
        catch (Exception error) { callback.complete(false, "Could not read wallpaper: " + error.getMessage()); }
    }

    static void generate(Context context, int drawableId, String space, Callback callback) {
        try {
            Drawable drawable = AppCompatResources.getDrawable(context, drawableId);
            if (drawable == null) throw new IllegalStateException("Wallpaper unavailable");
            Bitmap bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight()); drawable.draw(new Canvas(bitmap));
            process(context, InputImage.fromBitmap(bitmap, 0), space, callback);
        } catch (Exception error) { callback.complete(false, "Could not prepare wallpaper: " + error.getMessage()); }
    }

    private static void process(Context context, InputImage image, String space, Callback callback) {
        SubjectSegmenterOptions options = new SubjectSegmenterOptions.Builder().enableForegroundBitmap().build();
        SubjectSegmenter segmenter = SubjectSegmentation.getClient(options);
        segmenter.process(image).addOnSuccessListener(result -> {
            Bitmap foreground = result.getForegroundBitmap();
            if (foreground == null) { segmenter.close(); callback.complete(false, "No foreground subject was found"); return; }
            File directory = new File(context.getFilesDir(), "wallpaper_depth");
            if (!directory.exists() && !directory.mkdirs()) { segmenter.close(); callback.complete(false, "Could not create depth storage"); return; }
            File output = new File(directory, space + ".png");
            try (FileOutputStream stream = new FileOutputStream(output)) {
                foreground.compress(Bitmap.CompressFormat.PNG, 100, stream);
                context.getSharedPreferences("void", Context.MODE_PRIVATE).edit()
                    .putString("depth_" + space + "_path", output.getAbsolutePath()).apply();
                callback.complete(true, "Depth subject generated");
            } catch (Exception error) { callback.complete(false, "Could not save depth layer: " + error.getMessage()); }
            finally { segmenter.close(); }
        }).addOnFailureListener(error -> {
            segmenter.close();
            String detail = error.getMessage() == null ? "Model unavailable" : error.getMessage();
            callback.complete(false, "Depth engine: " + detail + ". Wait for the model download, then retry.");
        });
    }
}
