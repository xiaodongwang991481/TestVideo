#include <jni.h>
#include <string>

extern "C" {

#define LOG_TAG "android-ffmpeg-player"
#define LOGI(...) __android_log_print(4, LOG_TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(6, LOG_TAG, __VA_ARGS__);

#include "libavcodec/avcodec.h"

}

extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_xiaodong_testvideo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_configuration());
}


extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_xiaodong_testvideo_MainActivity_naInit(
        JNIEnv *env,
        jobject jobject1,
        jstring source
) {
    const char* camera_source = env->GetStringUTFChars(source, NULL);
    jstring ret_source = env->NewStringUTF(camera_source);
    env->ReleaseStringUTFChars(source, camera_source);
    return ret_source;

}
