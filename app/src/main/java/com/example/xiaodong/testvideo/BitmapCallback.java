package com.example.xiaodong.testvideo;

import android.graphics.Bitmap;

public interface BitmapCallback {
    void init(Camera camera);
    void bitmapCallback(Bitmap bitmap);
}
