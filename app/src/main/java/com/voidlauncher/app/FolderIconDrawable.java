package com.voidlauncher.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import java.util.List;

final class FolderIconDrawable extends Drawable {
    private final List<Drawable> icons;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    FolderIconDrawable(List<Drawable> icons) { this.icons = icons; paint.setColor(0xff383143); }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds(); float radius = Math.min(b.width(), b.height()) * .25f;
        canvas.drawRoundRect(b.left, b.top, b.right, b.bottom, radius, radius, paint);
        int gap = Math.max(2, b.width() / 14), cell = (b.width() - gap * 3) / 2;
        for (int i = 0; i < Math.min(4, icons.size()); i++) {
            int column = i % 2, row = i / 2;
            int left = b.left + gap + column * (cell + gap), top = b.top + gap + row * (cell + gap);
            Drawable icon = icons.get(i); icon.setBounds(left, top, left + cell, top + cell); icon.draw(canvas);
        }
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
