#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>
}

#define LOG_TAG "android-ffmpeg-player"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);

extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_configuration());
}



class CameraStreamHolder {
    jstring source;
    const char* camera_source;
    const int width;
    const int height;
    jobject bitmap;
    jobject callback;
    void* buffer;
    int 				videoStream = -1;
    JNIEnv * const env;
    AVFormatContext 	*formatCtx = NULL;
    AVCodecContext  	*codecCtx = NULL;
    AVFrame         	*decodedFrame = NULL;
    AVFrame         	*frameRGBA = NULL;
    struct SwsContext   *sws_ctx = NULL;
    AVCodec         *pCodec = NULL;

public:
    CameraStreamHolder(
            JNIEnv * const env, jstring source, const int width, const int height,
            jobject callback
    ) : env(env), source(source), camera_source(env->GetStringUTFChars(source, NULL)),
        width(width), height(height), bitmap(createBitmap()), callback(callback) {
    }

    void applyCallback() {
        if (env->IsSameObject(callback, NULL)) {
            return;
        }
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/CallbackFromJNI");
        jmethodID method = env->GetMethodID(cbClass, "bitmapCallback", "(Landroid/graphics/Bitmap;)V");
        env->CallVoidMethod(callback, method, bitmap);
    }

    jobject createBitmap() {
        //get Bitmap class and createBitmap method ID
        jclass javaBitmapClass = (jclass)env->FindClass("android/graphics/Bitmap");
        jmethodID mid = env->GetStaticMethodID(
                javaBitmapClass, "createBitmap",
                "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"
        );
        //create Bitmap.Config
        //reference: https://forums.oracle.com/thread/1548728
        jstring jConfigName = env->NewStringUTF("ARGB_8888");
        jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
        jobject javaBitmapConfig = env->CallStaticObjectMethod(
                bitmapConfigClass,
                env->GetStaticMethodID(
                        bitmapConfigClass, "valueOf",
                        "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;"
                ),
                jConfigName
        );
        //create the bitmap
        return env->CallStaticObjectMethod(
                javaBitmapClass, mid, width, height, javaBitmapConfig
        );
    }

    bool init() {
        // Open video file
        if (AndroidBitmap_lockPixels(env, bitmap, &buffer) < 0)
            return false;
        if(avformat_open_input(&formatCtx, camera_source, NULL, NULL) != 0){
            LOGE("Couldn't open input %s.\n", camera_source);
            return false;
        }
        // Retrieve stream information
        if(avformat_find_stream_info(formatCtx, NULL) < 0){
            LOGE("FAILED to find stream info %s", camera_source);
            return false; // Couldn't find stream information
        }
        // Dump information about file onto standard error
        av_dump_format(formatCtx, 0, camera_source, 0);
        for(int i = 0; i < formatCtx->nb_streams; i++) {
            if(formatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
                videoStream = i;
                break;
            }
        }
        if(videoStream == -1){
            LOGE("Didn't find a video stream");
            return false; // Didn't find a video stream
        }
        // Get a pointer to the codec context for the video stream
        codecCtx = formatCtx->streams[videoStream]->codec;
        // Find the decoder for the video stream
        pCodec = avcodec_find_decoder(codecCtx->codec_id);
        if(pCodec == NULL) {
            LOGE("Unsupported codec");
            return false; // Codec not found
        }
        // Open codec
        if(avcodec_open2(codecCtx, pCodec, NULL) < 0){
            LOGE("Could not open codec");
            return false; // Could not open codec
        }
        // Allocate video frame
        decodedFrame = av_frame_alloc();
        if (decodedFrame == NULL) {
            LOGE("AVFrame could not be allocated")
            return false;
        }
        // Allocate an AVFrame structure
        frameRGBA = av_frame_alloc();
        if(frameRGBA == NULL){
            LOGE("AVFrameRGBA could not be allocated");
            return false;
        }
        //get the scaling context
        sws_ctx = sws_getContext (
                codecCtx->width,
                codecCtx->height,
                codecCtx->pix_fmt,
                width,
                height,
                AV_PIX_FMT_RGBA,
                SWS_BILINEAR,
                NULL,
                NULL,
                NULL
        );
        avpicture_fill(
                (AVPicture *)frameRGBA, (const uint8_t *)buffer, AV_PIX_FMT_RGBA,
                width, height
        );
        return true;
    }

    bool process_packets() {
        int framefinished;
        AVPacket packet;
        while(av_read_frame(formatCtx, &packet) >= 0){
            if(packet.stream_index == videoStream){
                if(avcodec_decode_video2(
                        codecCtx, decodedFrame, &framefinished, &packet
                ) < 0) {
                    LOGE("Decode Error.\n");
                    av_free_packet(&packet);
                    return false;
                }
                if(framefinished){
                    sws_scale(sws_ctx, (const uint8_t* const*)decodedFrame->data,
                              decodedFrame->linesize, 0, codecCtx->height,
                              frameRGBA->data, frameRGBA->linesize
                    );
                    applyCallback();
                }
            }
            av_free_packet(&packet);
        }
        return true;
    }

    ~CameraStreamHolder() {
        if (camera_source != NULL) {
            env->ReleaseStringUTFChars(source, camera_source);
        }
        if (buffer != NULL) {
            AndroidBitmap_unlockPixels(env, bitmap);
            buffer = NULL;
        }
        if (sws_ctx != NULL) {
            sws_freeContext(sws_ctx);
            sws_ctx = NULL;
        }
        if (frameRGBA != NULL) {
            av_frame_free(&frameRGBA);
            frameRGBA = NULL;
        }
        if (decodedFrame != NULL) {
            av_frame_free(&decodedFrame);
            decodedFrame = NULL;
        }
        if (decodedFrame != NULL) {
            avcodec_close(codecCtx);
            decodedFrame = NULL;
        }
        if (formatCtx != NULL) {
            avformat_close_input(&formatCtx);
            formatCtx = NULL;
        }
    }
};


extern "C" JNIEXPORT jboolean
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_decode(
        JNIEnv *env,
        jobject jobject1,
        jstring source,
        jint width, jint height,
        jobject callback
) {
    CameraStreamHolder holder(env, source, width, height, callback);
    if(!holder.init()) {
        return false;
    }
    return holder.process_packets();
}
