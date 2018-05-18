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
Java_com_example_xiaodong_testvideo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_configuration());
}


int decode_internal(const char* camera_source, int width, int height, void* buffer) {
    AVFormatContext 	*formatCtx = NULL;
    int 				videoStream;
    AVCodecContext  	*codecCtx = NULL;
    AVFrame         	*decodedFrame = NULL;
    AVFrame         	*frameRGBA = NULL;
    struct SwsContext   *sws_ctx = NULL;
    int	stop;
    AVCodec         *pCodec = NULL;
    int 			i;
    AVDictionary *optionsDict = NULL;
    AVPacket packet;

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
        avformat_close_input(&formatCtx);
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
        avformat_close_input(&formatCtx);
        return -1; // Didn't find a video stream
    }
    // Get a pointer to the codec context for the video stream
    codecCtx = formatCtx->streams[videoStream]->codec;
    // Find the decoder for the video stream
    pCodec = avcodec_find_decoder(codecCtx->codec_id);
    if(pCodec == NULL) {
        LOGE("Unsupported codec");
        avformat_close_input(&formatCtx);
        return -1; // Codec not found
    }
    // Open codec
    if(avcodec_open2(codecCtx, pCodec, &optionsDict)<0){
        LOGE("Could not open codec");
        avcodec_close(codecCtx);
        avformat_close_input(&formatCtx);
        return -1; // Could not open codec
    }
    // Allocate video frame
    decodedFrame = av_frame_alloc();
    if (decodedFrame == NULL) {
        LOGE("AVFrame could not be allocated")
        avcodec_close(codecCtx);
        avformat_close_input(&formatCtx);
        return -1;
    }
    // Allocate an AVFrame structure
    frameRGBA = av_frame_alloc();
    if(frameRGBA == NULL){
        LOGE("AVFrameRGBA could not be allocated");
        av_frame_free(&decodedFrame);
        avcodec_close(codecCtx);
        avformat_close_input(&formatCtx);
        return -1;
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
            (AVPicture *)frameRGBA, buffer, AV_PIX_FMT_RGBA,
            width, height
    );
    int framefinished;
    while(av_read_frame(formatCtx, &packet)>=0){
        if(packet.stream_index==videoStream){
            if(avcodec_decode_video2(
                            codecCtx, decodedFrame, &framefinished, &packet
            ) < 0) {
                LOGE("Decode Error.\n");
                av_free_packet(&packet);
                av_frame_free(&frameRGBA);
                av_frame_free(&decodedFrame);
                avcodec_close(codecCtx);
                avformat_close_input(&formatCtx);
                return -1;
            }
            if(framefinished){
                sws_scale(sws_ctx, (const uint8_t* const*)decodedFrame->data,
                          decodedFrame->linesize, 0, codecCtx->height,
                          frameRGBA->data, frameRGBA->linesize
                );
            }
        }
        av_free_packet(&packet);
    }
    sws_freeContext(sws_ctx);
    av_frame_free(&frameRGBA);
    av_frame_free(&decodedFrame);
    avcodec_close(codecCtx);
    avformat_close_input(&formatCtx);
    return 0;
}

extern "C" JNIEXPORT jint
JNICALL
Java_com_example_xiaodong_testvideo_MainActivity_decode(
        JNIEnv *env,
        jobject jobject1,
        jstring source,
        jint width, jint height, jobject bitmap
) {

    const char* camera_source = env->GetStringUTFChars(source, NULL);
    void* buffer;
    if (AndroidBitmap_lockPixels(env, bitmap, &buffer) < 0) {
        env->ReleaseStringUTFChars(source, camera_source);
        return -1;
    }
    int ret = decode_internal(camera_source, width, height, buffer);
    AndroidBitmap_unlockPixels(env, bitmap);
    env->ReleaseStringUTFChars(source, camera_source);
    return 0;
}
