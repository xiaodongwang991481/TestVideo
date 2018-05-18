#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "libavcodec/avcodec.h"
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>
}

#define LOG_TAG "android-ffmpeg-player"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);

extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_xiaodong_testvideo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_configuration());
}


int decode_internal(const char* camera_source) {
    AVFormatContext 	*formatCtx = NULL;
    int 				videoStream;
    AVCodecContext  	*codecCtx = NULL;
    AVFrame         	*decodedFrame = NULL;
    AVFrame         	*frameRGBA = NULL;
    jobject				bitmap;
    void*				buffer;
    struct SwsContext   *sws_ctx = NULL;
    int 				width;
    int 				height;
    int	stop;
    AVCodec         *pCodec = NULL;
    int 			i;
    AVDictionary *optionsDict = NULL;

    av_register_all();
    avformat_network_init();
    // Open video file
    if(avformat_open_input(&formatCtx, camera_source, NULL, NULL)!=0){
        LOGE("Couldn't open input %s.\n", camera_source);
        return -1;
    }
    // Retrieve stream information
    if(avformat_find_stream_info(formatCtx, NULL)<0){
        LOGE("FAILED to find stream info %s", camera_source);
        return -1; // Couldn't find stream information
    }
    // Dump information about file onto standard error
    av_dump_format(formatCtx, 0, camera_source, 0);
    videoStream=-1;
    for(int i=0; i<formatCtx->nb_streams; i++) {
        if(formatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO) {
            videoStream=i;
            break;
        }
    }
    if(videoStream==-1){
        LOGE("Didn't find a video stream");
        return -1; // Didn't find a video stream
    }
    // Get a pointer to the codec context for the video stream
    codecCtx=formatCtx->streams[videoStream]->codec;
    // Find the decoder for the video stream
    pCodec=avcodec_find_decoder(codecCtx->codec_id);
    if(pCodec == NULL) {
        LOGE("Unsupported codec");
        return -1; // Codec not found
    }
    // Open codec
    if(avcodec_open2(codecCtx, pCodec, &optionsDict)<0){
        LOGE("Could not open codec");
        return -1; // Could not open codec
    }
    // Allocate video frame
    decodedFrame = av_frame_alloc();
    // Allocate an AVFrame structure
    frameRGBA = av_frame_alloc();
    if(frameRGBA == NULL){
        LOGE("AVFrame could not be allocated");
        return -1;
    }
    return 0;
}

extern "C" JNIEXPORT jint
JNICALL
Java_com_example_xiaodong_testvideo_MainActivity_decode(
        JNIEnv *env,
        jobject jobject1,
        jstring source
) {

    const char* camera_source = env->GetStringUTFChars(source, NULL);
    int ret = decode_internal(camera_source);
    env->ReleaseStringUTFChars(source, camera_source);
    return 0;
}
