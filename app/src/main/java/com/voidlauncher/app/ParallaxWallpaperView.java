package com.voidlauncher.app;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public final class ParallaxWallpaperView extends AppCompatImageView {
    private final Matrix wallpaperMatrix = new Matrix();
    private float zoom = 1.10f, focalY = 0.5f, offsetX, offsetY;

    public ParallaxWallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
    }

    public void configure(float zoomValue, float focalValue) {
        zoom = Math.max(1f, Math.min(1.30f, zoomValue));
        focalY = Math.max(0f, Math.min(1f, focalValue));
        updateMatrix();
    }

    public void setMotion(float x, float y) {
        offsetX = x; offsetY = y; updateMatrix();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight); updateMatrix();
    }

    @Override public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable); post(this::updateMatrix);
    }

    private void updateMatrix() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() <= 0 || getHeight() <= 0) return;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            drawable.setBounds(0, 0, getWidth(), getHeight()); wallpaperMatrix.reset(); setImageMatrix(wallpaperMatrix); return;
        }
        float dw = drawable.getIntrinsicWidth(), dh = drawable.getIntrinsicHeight();
        float scale = Math.max(getWidth() / dw, getHeight() / dh) * zoom;
        float scaledWidth = dw * scale, scaledHeight = dh * scale;
        float extraX = Math.max(0, scaledWidth - getWidth()), extraY = Math.max(0, scaledHeight - getHeight());
        float x = clamp(-extraX / 2f + offsetX, -extraX, 0f);
        float y = clamp(-extraY * focalY + offsetY, -extraY, 0f);
        wallpaperMatrix.reset(); wallpaperMatrix.setScale(scale, scale); wallpaperMatrix.postTranslate(x, y);
        setImageMatrix(wallpaperMatrix);
    }

    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
