#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <vector>
#include <utility>

using namespace std;

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>
#include <libavdevice/avdevice.h>
#include <libavutil/dict.h>
}

#define LOG_TAG "android-ffmpeg"
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
    vector<jstring> dests;
	vector<const char*> camera_dests;
    vector<AVFormatContext*> oformatCtxs;

    int width;
    int height;
    jobject bitmap;
    jobject callback;
    void* buffer;
    bool initialized = false;
    int 				videoStream = -1;
    JNIEnv * const env;
    AVFormatContext 	*formatCtx = NULL;
	AVInputFormat *inputFormat = NULL;
	AVDictionary *options = NULL;
    AVCodecContext  	*codecCtx = NULL;
    AVFrame         	*decodedFrame = NULL;
    AVFrame         	*frameRGBA = NULL;
    struct SwsContext   *sws_ctx = NULL;
    AVCodec         *pCodec = NULL;

public:
    CameraStreamHolder(
            JNIEnv * const env, jstring source, jobjectArray dests,
            const int width, const int height,
            jobject callback
    ) : env(env), source(source), camera_source(env->GetStringUTFChars(source, NULL)),
        width(width), height(height), callback(callback) {
        if (!env->IsSameObject(dests, NULL)) {
            int destLength = env->GetArrayLength(dests);
            for (int i = 0; i < destLength; i++) {
                jstring dest = (jstring)(env->GetObjectArrayElement(dests, i));
                const char *camera_dest = env->GetStringUTFChars(dest, NULL);
                this->dests.push_back(dest);
				camera_dests.push_back(camera_dest);
                oformatCtxs.push_back(NULL);
            }
        }
    }

    void applyBitmapCallback() {
        if (env->IsSameObject(callback, NULL)) {
            return;
        }
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/CallbackFromJNI");
        jmethodID method = env->GetMethodID(cbClass, "bitmapCallback", "(Landroid/graphics/Bitmap;)V");
        env->CallVoidMethod(callback, method, bitmap);
    }

    bool applyFinishCallback() {
        if (env->IsSameObject(callback, NULL)) {
            return true;
        }
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/CallbackFromJNI");
        jmethodID method = env->GetMethodID(cbClass, "finishCallback", "()Z");
        return env->CallBooleanMethod(callback, method);
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

    void copy_stream_info(AVStream* ostream, AVStream* istream, AVFormatContext* ofmt_ctx){
        AVCodecContext* icodec = istream->codec;
        AVCodecContext* ocodec = ostream->codec;

        ostream->id = istream->id;
        ocodec->codec_id = icodec->codec_id;
        ocodec->codec_type = icodec->codec_type;
        ocodec->bit_rate = icodec->bit_rate;

        int extra_size = (uint64_t)icodec->extradata_size + AV_INPUT_BUFFER_PADDING_SIZE;
        ocodec->extradata = (uint8_t*)av_mallocz(extra_size);
        memcpy(ocodec->extradata, icodec->extradata, icodec->extradata_size);
        ocodec->extradata_size= icodec->extradata_size;

        // Some formats want stream headers to be separate.
        if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER){
            ostream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
        }
    }

    void copy_video_stream_info(AVStream* ostream, AVStream* istream, AVFormatContext* ofmt_ctx){
        copy_stream_info(ostream, istream, ofmt_ctx);

        AVCodecContext* icodec = istream->codec;
        AVCodecContext* ocodec = ostream->codec;

        ocodec->width = icodec->width;
        ocodec->height = icodec->height;
        ocodec->time_base = icodec->time_base;
        ocodec->gop_size = icodec->gop_size;
        ocodec->pix_fmt = icodec->pix_fmt;
    }

    void copy_audio_stream_info(AVStream* ostream, AVStream* istream, AVFormatContext* ofmt_ctx){
        copy_stream_info(ostream, istream, ofmt_ctx);

        AVCodecContext* icodec = istream->codec;
        AVCodecContext* ocodec = ostream->codec;

        ocodec->sample_fmt = icodec->sample_fmt;
        ocodec->sample_rate = icodec->sample_rate;
        ocodec->channels = icodec->channels;
    }

    bool init() {
        if (initialized) {
            LOGI("is already initialized.\n")
            return true;
        }
		const char* device_prefix = "/dev/";
		int camera_source_len = strlen(camera_source);
		int device_prefix_len = strlen(device_prefix);
		if (
		    camera_source_len >= device_prefix_len &&
			strncmp (camera_source, device_prefix, device_prefix_len) == 0
		) {
			inputFormat = av_find_input_format("v4l2");
			av_dict_set(&options, "framerate", "20", 0);
		}
        // Open video file
        if(avformat_open_input(&formatCtx, camera_source, inputFormat, &options) != 0){
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
        videoStream = av_find_best_stream(formatCtx, AVMEDIA_TYPE_VIDEO, -1, -1, &pCodec, 0);
        if (videoStream < 0) {
            LOGE("Didn't find a video stream");
            return false; // Didn't find a video stream
        }
        // Get a pointer to the codec context for the video stream
        codecCtx = avcodec_alloc_context3(pCodec);

        if(avcodec_copy_context(codecCtx, formatCtx->streams[videoStream]->codec) < 0) {
            fprintf(stderr, "Couldn't copy codec context");
            return false; // Error copying codec context
        }
        if (width == 0) {
            width = codecCtx->width;
        }
        if (height == 0) {
            height = codecCtx->height;
        }
        bitmap = createBitmap();
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
        AVStream* istream = formatCtx->streams[videoStream];
        int destLength = dests.size();
        for (int i = 0; i < destLength; i++) {
            const char* camera_dest = camera_dests[i];
            if(avformat_alloc_output_context2(&oformatCtxs[i], NULL, "flv", camera_dest) < 0){
                LOGE("oformatCtx %s is failed to alloc.\n", camera_dest);
                return false;
            }
            AVFormatContext* oformatCtx = oformatCtxs[i];
            if(avio_open2(
                    &(oformatCtx->pb), camera_dest,
                    AVIO_FLAG_WRITE, &(oformatCtx->interrupt_callback), NULL
            ) < 0){
                LOGE("failed to open AVIO: %s.\n", camera_dest)
                return false;
            }
            AVStream* ostream = avformat_new_stream(oformatCtx, NULL);
            if (ostream == NULL) {
                LOGE("failed to create stream: %s.\n", camera_dest);
                return false;
            }
            copy_video_stream_info(ostream, istream, oformatCtx);
            av_dump_format(oformatCtx, 0, camera_dest, 1);
            if(avformat_write_header(oformatCtx, NULL) != 0){
                LOGE("failed to write header: %s.\n", camera_dest);
                return false;
            }
        }
        if (AndroidBitmap_lockPixels(env, bitmap, &buffer) < 0)
            return false;
        avpicture_fill(
                (AVPicture *)frameRGBA, (const uint8_t *)buffer, AV_PIX_FMT_RGBA,
                width, height
        );
        initialized = true;
        return true;
    }

    bool process_packets(bool decode=true, bool sync=false) {
        AVPacket packet;
        av_init_packet(&packet);
        while(av_read_frame(formatCtx, &packet) >= 0){
            if(packet.stream_index == videoStream){
                if (decode) {
                    decode_packet(packet);
                }
                int destLength = dests.size();
                for (int i = 0; i < destLength; i++) {
                    const char* camera_dest = camera_dests[i];
                    AVFormatContext* oformatCtx = oformatCtxs[i];
                    if(av_interleaved_write_frame(oformatCtx, &packet) < 0) {
                        LOGE("failed to write frame: %s.\n", camera_dest);
                    }
                }
            } else {
                LOGI("ignore stream %d.\n", packet.stream_index)
            }
            av_free_packet(&packet);
            if(applyFinishCallback()) {
                break;
            }
        }
        if (decode) {
            while (1) {
                if (!decode_packet(packet)) {
                    break;
                }
            }
        }
		int destLength = dests.size();
        for (int i = 0; i < destLength; i++) {
            const char* camera_dest = camera_dests[i];
            AVFormatContext* oformatCtx = oformatCtxs[i];
            if(av_write_trailer(oformatCtx) < 0) {
                LOGE("failed to write trailer: %s.\n", camera_dest);
            }
        }
        return true;
    }

    bool decode_packet(AVPacket& packet) {
        AVStream* istream = formatCtx->streams[videoStream];
        double time_ms = 0;
        if (istream->time_base.den > 0) {
            time_ms = packet.pts * istream->time_base.num * 1000 / istream->time_base.den;
        }
        LOGI("packet pts=%ld dts=%ld duration=$ld.\n", packet.pts, packet.dts, packet.duration);
        LOGI("time ms=%lf.\n", time_ms);
        // if (sync) {
        //     av_usleep(time_ms * 1000);
        // }
        int frameFinished = 0;
        if (avcodec_decode_video2(
                codecCtx, decodedFrame, &frameFinished, &packet
        ) < 0) {
            LOGE("Decode Error.\n");
            return false;
        } else if (frameFinished) {
            LOGI("packet after decode pts=%ld dts=%ld duration=$ld.\n", packet.pts, packet.dts, packet.duration);
            sws_scale(sws_ctx, (const uint8_t *const *) decodedFrame->data,
                      decodedFrame->linesize, 0, codecCtx->height,
                      frameRGBA->data, frameRGBA->linesize
            );
            applyBitmapCallback();
        } else {
            LOGE("Frame not finished.\n");
            return false;
        }
        return true;
    }

    ~CameraStreamHolder() {
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
        if (codecCtx != NULL) {
            avcodec_close(codecCtx);
            decodedFrame = NULL;
        }
        pCodec = NULL;
        if (formatCtx != NULL) {
            avformat_close_input(&formatCtx);
            formatCtx = NULL;
        }
		if (options != NULL) {
			av_dict_free(&options);
			options = NULL;
		}
        if (camera_source != NULL) {
            env->ReleaseStringUTFChars(source, camera_source);
        }
        int destLength = dests.size();
        for (int i = 0; i < destLength; i++) {
            avformat_free_context(oformatCtxs[i]);
        }
        for (int i = 0; i < destLength; i++) {
            env->ReleaseStringUTFChars(dests[i], camera_dests[i]);
        }
    }
};


extern "C" JNIEXPORT jboolean
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_decode(
        JNIEnv *env,
        jobject jobject1,
        jstring source,
        jobjectArray dests,
        jint width, jint height,
        jobject callback,
        jboolean decode,
        jboolean sync
) {
    CameraStreamHolder holder(env, source, dests, width, height, callback);
    if(!holder.init()) {
        return false;
    }
    return holder.process_packets(decode, sync);
}

extern "C" JNIEXPORT void
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_initJNI(
        JNIEnv *env,
        jclass jclass1
) {
	avdevice_register_all(); /* for device & add libavdevice/avdevice.h headerfile*/
    avcodec_register_all();
    av_register_all();
    avformat_network_init();
}
