#include <jni.h>
#include <string>

extern "C" {
#include "libavcodec/avcodec.h"
}

extern "C" JNIEXPORT jstring

JNICALL
Java_com_example_xiaodong_testvideo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_configuration());
}
