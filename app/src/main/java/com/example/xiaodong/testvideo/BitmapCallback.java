package com.example.xiaodong.testvideo;

import android.graphics.Bitmap;

public interface BitmapCallback {
    void bitmapCallback(Bitmap bitmap);
    boolean shouldSync();
}
