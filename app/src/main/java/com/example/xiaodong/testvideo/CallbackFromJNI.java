package com.example.xiaodong.testvideo;

import android.graphics.Bitmap;

public interface CallbackFromJNI {
    void bitmapCallback(Bitmap bitmap);
    boolean finishCallback();
    boolean shouldSync();
}
