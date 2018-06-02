#include <jni.h>
#include <string>
#include <vector>
#include <utility>
#include <android/log.h>
#include <android/bitmap.h>

using namespace std;

extern "C" {
#define __STDC_CONSTANT_MACROS
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>
#include <libavdevice/avdevice.h>
#include <libavutil/dict.h>
#include <libavutil/time.h>
}

#define LOG_TAG "android-ffmpeg"
#define LOGV(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);
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
    vector<AVCodecContext*> ocodecCtxs;
    vector<AVStream*> ostreams;

    int width = 0;
    int height = 0;
    jobject callback = NULL;
    jmethodID bitmapCallbackMethod = NULL;
    jmethodID finishCallbackMethod = NULL;
    bool decode = false;
    bool sync = false;
    jobject bitmap = NULL;
    void* buffer = NULL;
    bool initialized = false;
    JNIEnv * const env;
    AVFormatContext 	*formatCtx = NULL;
	AVInputFormat *inputFormat = NULL;
	AVDictionary *options = NULL;
    AVCodecContext  	*codecCtx = NULL;
    AVFrame         	*decodedFrame = NULL;
    AVFrame         	*frameRGBA = NULL;
    AVStream            *videoStream = NULL;
    int                 videoStreamIndex = -1;
    struct SwsContext   *sws_ctx = NULL;
    AVCodec         *pCodec = NULL;

public:
    CameraStreamHolder(
            JNIEnv * const env, jstring source, jobjectArray dests,
            jobject callback
    ) : env(env), source(source), camera_source(env->GetStringUTFChars(source, NULL)) {
        LOGI("create camera holder source: %s.\n", camera_source);
        if (!env->IsSameObject(callback, NULL)) {
            this->callback = env->NewGlobalRef(callback);
            decode = true;
        }
        if (!env->IsSameObject(dests, NULL)) {
            int destLength = env->GetArrayLength(dests);
            for (int i = 0; i < destLength; i++) {
                jstring dest = (jstring)(env->GetObjectArrayElement(dests, i));
                const char *camera_dest = env->GetStringUTFChars(dest, NULL);
                LOGI("create camer dest %s for source %s.\n", camera_dest, camera_source);
                this->dests.push_back(dest);
				camera_dests.push_back(camera_dest);
                oformatCtxs.push_back(NULL);
                ocodecCtxs.push_back(NULL);
                ostreams.push_back(NULL);
            }
        }
    }

    bool initBitmapCallbackMethod() {
        LOGI("init bitmapCallback method.\n");
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/CallbackFromJNI");
        if (env->IsSameObject(cbClass, NULL)) {
            LOGE("failed to get CallbackFromJNI class.\n");
            return false;
        }
        bitmapCallbackMethod = env->GetMethodID(cbClass, "bitmapCallback", "(Landroid/graphics/Bitmap;)V");
        if (bitmapCallbackMethod == NULL) {
            LOGE("failed to get bitmapCallback method.\n");
            return false;
        }
        LOGI("got bitmapCallback method.\n");
        return true;
    }

    void applyBitmapCallback() {
        LOGV("apply bitmap callback on %s.\n", camera_source);
        if (env->IsSameObject(callback, NULL)) {
            LOGE("callback is NULL.\n");
            return;
        }
        env->CallVoidMethod(callback, bitmapCallbackMethod, bitmap);
        if (env->ExceptionCheck()) {
            LOGE("Exception occurs when call bitmapCallback.\n");
        }
    }

    bool initFinishCallbackMethod() {
        LOGI("init finish callback method.\n");
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/CallbackFromJNI");
        if (env->IsSameObject(cbClass, NULL)) {
            LOGE("failed to get CallbackFromJNI class.\n");
            return false;
        }
        finishCallbackMethod = env->GetMethodID(cbClass, "finishCallback", "()Z");
        if (finishCallbackMethod == NULL) {
            LOGE("failed to get finishCallback method.\n");
            return false;
        }
        LOGI("got finish callback method.\n");
        return true;
    }

    bool applyFinishCallback() {
        LOGV("apply finish callback on %s.\n", camera_source);
        if (env->IsSameObject(callback, NULL)) {
            LOGE("callback is NULL");
            return true;
        }
        jboolean finished = env->CallBooleanMethod(callback, finishCallbackMethod);
        if (env->ExceptionCheck()) {
            LOGE("exception occurs when calling finishCallback.\n");
            return true;
        }
        LOGV("should be finished? %d.\n", finished);
        return finished;
    }

    bool Sync() {
        LOGI("init sync callback method.\n");
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/CallbackFromJNI");
        if (env->IsSameObject(cbClass, NULL)) {
            LOGE("failed to get CallbackFromJNI class.\n");
            return false;
        }
        jmethodID  syncCallbackMethod = env->GetMethodID(cbClass, "shouldSync", "()Z");
        if (syncCallbackMethod == NULL) {
            LOGE("failed to get syncCallback method.\n");
            return false;
        }
        if (env->IsSameObject(callback, NULL)) {
            LOGE("callback is NULL");
            return true;
        }
        jboolean sync = env->CallBooleanMethod(callback, syncCallbackMethod);
        if (env->ExceptionCheck()) {
            LOGE("exception occurs when calling syncCallback.\n");
            return true;
        }
        LOGI("should be in sync? %d.\n", sync);
        return sync;
    }

    jobject createBitmap() {
        LOGI("create bitmap.\n");
        //get Bitmap class and createBitmap method ID
        jclass javaBitmapClass = (jclass)env->FindClass("android/graphics/Bitmap");
        if (env->IsSameObject(javaBitmapClass, NULL)) {
            LOGE("Failed to get Bitmap Class.\n");
            return NULL;
        }
        jmethodID mid = env->GetStaticMethodID(
                javaBitmapClass, "createBitmap",
                "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"
        );
        if (mid == NULL) {
            LOGE("failed to get CreateBitmap method.\n");
            return NULL;
        }
        //create Bitmap.Config
        //reference: https://forums.oracle.com/thread/1548728
        jstring jConfigName = env->NewStringUTF("ARGB_8888");
        jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
        if (env->IsSameObject(bitmapConfigClass, NULL)) {
            LOGE("failed to get Bitmap$Config class.\n");
            return NULL;
        }
        jobject javaBitmapConfig = env->CallStaticObjectMethod(
                bitmapConfigClass,
                env->GetStaticMethodID(
                        bitmapConfigClass, "valueOf",
                        "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;"
                ),
                jConfigName
        );
        if (env->IsSameObject(javaBitmapConfig, NULL)) {
            LOGE("failed to get Bitmap$Config instance.\n");
            return NULL;
        }
        //create the bitmap
        jobject localBitmap = env->CallStaticObjectMethod(
                javaBitmapClass, mid, width, height, javaBitmapConfig
        );
        if (env->IsSameObject(localBitmap, NULL)) {
            LOGE("failed to get Bitmap instance.\n");
            return NULL;
        }
        return reinterpret_cast<jobject>(env->NewGlobalRef(localBitmap));
    }

    void copy_codec_info(AVCodecContext* ocodec, AVCodecContext* icodec, AVFormatContext* ofmt_ctx) {
        ocodec->codec_id = icodec->codec_id;
        ocodec->codec_type = icodec->codec_type;
        ocodec->bit_rate = icodec->bit_rate;
        int extra_size = (uint64_t)icodec->extradata_size + AV_INPUT_BUFFER_PADDING_SIZE;
        ocodec->extradata = (uint8_t*)av_mallocz(extra_size);
        memcpy(ocodec->extradata, icodec->extradata, icodec->extradata_size);
        ocodec->extradata_size= icodec->extradata_size;

        // Some formats want stream headers to be separate.
        ocodec->codec_tag = 0;
        if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER){
            ocodec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
        }
    }

    void copy_stream_info(AVStream* ostream, AVStream* istream, AVFormatContext* ofmt_ctx){
        ostream->id = istream->id;
        // avcodec_parameters_copy(ostream->codecpar, istream->codecpar);
    }

    void copy_video_codec_info(AVCodecContext* ocodec, AVCodecContext* icodec, AVFormatContext* ofmt_ctx) {
        copy_codec_info(ocodec, icodec, ofmt_ctx);
        ocodec->width = icodec->width;
        ocodec->height = icodec->height;
        ocodec->time_base = icodec->time_base;
        ocodec->framerate = icodec->framerate;
        ocodec->gop_size = icodec->gop_size;
        ocodec->pix_fmt = icodec->pix_fmt;
    }

    void copy_audio_codec_info(AVCodecContext* ocodec, AVCodecContext* icodec, AVFormatContext* ofmt_ctx) {
        copy_codec_info(ocodec, icodec, ofmt_ctx);
        ocodec->sample_fmt = icodec->sample_fmt;
        ocodec->sample_rate = icodec->sample_rate;
        ocodec->channels = icodec->channels;
    }

    void copy_video_stream_info(AVStream* ostream, AVStream* istream, AVFormatContext* ofmt_ctx){
        copy_video_codec_info(ostream->codec, istream->codec, ofmt_ctx);
        copy_stream_info(ostream, istream, ofmt_ctx);
        ostream->time_base = istream->time_base;
        ostream->r_frame_rate = istream->r_frame_rate;
        ostream->avg_frame_rate = istream->avg_frame_rate;
        ostream->duration = istream->duration;
    }

    void copy_audio_stream_info(AVStream* ostream, AVStream* istream, AVFormatContext* ofmt_ctx){
        copy_audio_codec_info(ostream->codec, istream->codec, ofmt_ctx);
        copy_stream_info(ostream, istream, ofmt_ctx);
    }

    void check_error(int err_code) {
        void* buf = av_malloc(1024);
        if (buf != NULL) {
            av_strerror(err_code, (char *) buf, 1024);
            LOGE("error code %d: %s.\n", err_code, buf);
            av_free(buf);
        } else {
            LOGE("failed to alloc buf to print err message.\n");
        }
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
        int err_code;
        if((err_code=avformat_open_input(&formatCtx, camera_source, inputFormat, &options)) < 0){
            LOGE("Couldn't open input %s.\n", camera_source);
            check_error(err_code);
            return false;
        }
        LOGI("camera input %s is opened.\n", camera_source);
        // Retrieve stream information
        if((err_code=avformat_find_stream_info(formatCtx, NULL)) < 0){
            LOGE("FAILED to find stream info %s.\n", camera_source);
            check_error(err_code);
            return false; // Couldn't find stream information
        }
        LOGI("read camera %s stream info success.\n", camera_source);
        // Dump information about file onto standard error
        av_dump_format(formatCtx, 0, camera_source, 0);
        err_code = av_find_best_stream(formatCtx, AVMEDIA_TYPE_VIDEO, -1, -1, &pCodec, 0);
        if (err_code < 0) {
            LOGE("Didn't find a video stream.\n");
            check_error(err_code);
            return false; // Didn't find a video stream
        }
        videoStreamIndex = err_code;
        videoStream = formatCtx->streams[err_code];
        if (videoStream == NULL) {
            LOGE("failed to get video stream.\n");
            return false;
        }
        LOGI("find video stream %d.\n", videoStreamIndex);
        if(pCodec == NULL) {
            LOGE("Unsupported codec");
            return false; // Codec not found
        }
        LOGI("get codec success.\n")
        // Get a pointer to the codec context for the video stream
        codecCtx = videoStream->codec;
        LOGI("get codec context success.\n")
        width = codecCtx->width;
        LOGI("get width %d.\n", width);
        height = codecCtx->height;
        LOGI("get height %d.\n", height);
        double timebase = av_q2d(codecCtx->time_base);
        LOGI("get codec timebase %lf.\n", timebase);
        int64_t bit_rate = codecCtx->bit_rate;
        LOGI("get codec bit rate %ld.\n", bit_rate);
        double framerate = av_q2d(codecCtx->framerate);
        LOGI("get codec framerate %lf.\n", framerate);
        double stream_timebase = av_q2d(videoStream->time_base);
        LOGI("get stream timebase %lf.\n", stream_timebase);
        double r_framerate = av_q2d(videoStream->r_frame_rate);
        LOGI("get stream r framerate %lf.\n", r_framerate);
        double avg_framerate = av_q2d(videoStream->avg_frame_rate);
        LOGI("get stream avg framerate %lf.\n", avg_framerate);
        bitmap = createBitmap();
        if (env->IsSameObject(bitmap, NULL)) {
            LOGE("failed to create bitmap.\n");
            return false;
        }
        LOGI("bitmap instance is created.\n");
        if (!initBitmapCallbackMethod()) {
            LOGE("failed to init bitmap callback method.\n");
            return false;
        }
        LOGI("bitmap callback methed is got.\n");
        if (!initFinishCallbackMethod()) {
            LOGE("failed to init finish callback method.\n");
            return false;
        }
        LOGI("finish callback method is got.n");
        sync = Sync();
        LOGI("sync=%d", sync)
        // Open codec
        if((err_code = avcodec_open2(codecCtx, pCodec, NULL)) < 0) {
            LOGE("Could not open codec.\n");
            check_error(err_code);
            return false; // Could not open codec
        }
        LOGI("open codec success.\n");
        // Allocate video frame
        decodedFrame = av_frame_alloc();
        if (decodedFrame == NULL) {
            LOGE("AVFrame could not be allocated.\n")
            return false;
        }
        LOGI("alloc decoded frame success.\n");
        // Allocate an AVFrame structure
        frameRGBA = av_frame_alloc();
        if(frameRGBA == NULL){
            LOGE("AVFrameRGBA could not be allocated.\n");
            return false;
        }
        LOGI("alloc rgba frame success.\n");
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
        LOGI("got sws context.\n");
        int destLength = dests.size();
        for (int i = 0; i < destLength; i++) {
            const char* camera_dest = camera_dests[i];
            if((err_code = avformat_alloc_output_context2(
                    &oformatCtxs[i], NULL, "flv", camera_dest)
               ) < 0) {
                LOGE("oformatCtx %s is failed to alloc.\n", camera_dest);
                check_error(err_code);
                return false;
            }
            LOGI("alloc output context for %s.\n", camera_dest);
            AVFormatContext* oformatCtx = oformatCtxs[i];
            if (!(oformatCtx->flags & AVFMT_NOFILE)) {
                if ((err_code = avio_open2(
                        &(oformatCtx->pb), camera_dest,
                        AVIO_FLAG_WRITE, &(oformatCtx->interrupt_callback), NULL
                )) < 0) {
                    LOGE("failed to open AVIO: %s.\n", camera_dest);
                    check_error(err_code);
                    return false;
                }
                if (oformatCtx->pb == NULL) {
                    LOGE("failed to alloc pb: %s.\n", camera_dest);
                    return false;
                }
            } else {
                LOGI("No need to open avio: %s.\n", camera_dest);
            }
            LOGI("open avio for %s success.\n", camera_dest);
            AVStream* ostream = avformat_new_stream(oformatCtx, NULL);
            if (ostream == NULL) {
                LOGE("failed to create stream: %s.\n", camera_dest);
                return false;
            }
            ostreams[i] = ostream;
            AVCodecContext* ocodecCtx = ostream->codec;
            ocodecCtxs[i] = ocodecCtx;
            if (ocodecCtx == NULL) {
                LOGE("failed to get output codec context for %s.\n", camera_dest);
                return false;
            }
            LOGI("open oput stream for %s success.\n", camera_dest);
            if ((err_code = avcodec_copy_context(ocodecCtx, codecCtx)) < 0) {
                LOGE("failed to copy codec context to %s.\n", camera_dest);
                check_error(err_code);
                return false;
            }
            copy_video_stream_info(ostream, videoStream, oformatCtx);
            av_dump_format(oformatCtx, 0, camera_dest, 1);
        }
        if ((err_code = AndroidBitmap_lockPixels(env, bitmap, &buffer)) < 0) {
            LOGE("failed to lock pixels.\n");
            check_error(err_code);
            return false;
        }
        LOGI("lock buffer %p.\n", buffer);
        avpicture_fill(
                (AVPicture *)frameRGBA, (const uint8_t *)buffer, AV_PIX_FMT_RGBA,
                width, height
        );
        LOGI("fill picture with buffer.\n");
        initialized = true;
        return true;
    }

    bool process_packets(int64_t last_pts) {
        LOGI("process packets with decode=%d sync=%d", decode, sync);
        AVPacket packet;
        av_init_packet(&packet);
        int64_t base_time_ms = av_gettime();
        int64_t base_pts = last_pts;
        int err_code;
        int destLength = dests.size();
        for (int i = 0; i < destLength; i++) {
            const char *camera_dest = camera_dests[i];
            AVFormatContext *oformatCtx = oformatCtxs[i];
            if ((err_code = avformat_write_header(oformatCtx, NULL)) < 0) {
                LOGE("failed to write header: %s.\n", camera_dest);
                check_error(err_code);
            } else {
                LOGI("write header to %s.\n", camera_dest);
            }
        }
        if ((err_code = av_seek_frame(formatCtx, videoStreamIndex, last_pts, 0)) < 0) {
            LOGE("failed to seek frame at %ld.\n", last_pts);
            check_error(err_code);
            return last_pts;
        }
        while(true){
            if ((err_code =av_read_frame(formatCtx, &packet)) < 0) {
                LOGE("Failed to read frame.\n");
                check_error(err_code);
                break;
            }
            if(packet.stream_index == videoStreamIndex){
                LOGV("packet %d demux pts=%ld dts=%ld.\n", codecCtx->frame_number, packet.pts, packet.dts);
                last_pts = packet.pts;
                if (decode) {
                    LOGV(
                            "decode one packet at base time ms=%ld base pts=%ld.\n",
                            base_time_ms, base_pts
                    )
                    if(!decode_packet(packet, &base_time_ms, &base_pts)) {
                        LOGE(
                                "failed to decode packet at base time=%ld base pts=%ld.\n",
                                base_time_ms, base_pts
                        );
                    }
                }
                for (int i = 0; i < destLength; i++) {
                    const char* camera_dest = camera_dests[i];
                    AVFormatContext* oformatCtx = oformatCtxs[i];
                    AVCodecContext* ocodecCtx = ocodecCtxs[i];
                    LOGV("packet %d mux pts=%ld dts=%ld.\n", ocodecCtx->frame_number, packet.pts, packet.dts);
                    if((err_code = av_write_frame(oformatCtx, &packet)) < 0) {
                        LOGE("failed to write frame: %s.\n", camera_dest);
                        check_error(err_code);
                    } else {
                        LOGV("write one frame: %s.\n", camera_dest);
                    }
                }
            } else {
                LOGV("ignore stream %d.\n", packet.stream_index)
            }
            av_free_packet(&packet);
            // packet = AVPacket();
            if(applyFinishCallback()) {
                LOGI("frame parsing is finished early.\n");
                break;
            }
        }
        for (int i = 0; i < destLength; i++) {
            const char* camera_dest = camera_dests[i];
            AVFormatContext* oformatCtx = oformatCtxs[i];
            int err_code;
            if((err_code = av_write_trailer(oformatCtx)) < 0) {
                LOGE("failed to write trailer: %s.\n", camera_dest);
                check_error(err_code);
            } else {
                LOGI("write trailer: %s.\n", camera_dest);
            }
        }
        return last_pts;
    }

    bool decode_packet(AVPacket& packet, int64_t* base_time_ms, int64_t* base_pts) {
        AVRational ms_rational = av_make_q(1, 1000000);
        int err_code;
        if ((err_code = avcodec_send_packet(codecCtx, &packet)) < 0) {
            if (err_code != EAGAIN) {
                LOGE("failed to send packet.\n");
                check_error(err_code);
                return false;
            }
        }
        LOGV("send one packet.\n");
        while (true) {
            if ((err_code = avcodec_receive_frame(
                    codecCtx, decodedFrame
            )) < 0) {
                if (err_code != AVERROR(EAGAIN)) {
                    LOGE("failed to receive packet.\n");
                    check_error(err_code);
                    return false;
                }
                break;
            }
            LOGV("receive one packet.\n");
            if (sync) {
                int64_t time_ms_wait = 0;
                int64_t time_ms_current = av_gettime();
                int64_t time_ms_elapsed = time_ms_current - *base_time_ms;
                int64_t time_ms_duration = av_rescale_q(
                        (packet.pts - *base_pts), videoStream->time_base, ms_rational
                );
                LOGV("time duration: %ld.\n", time_ms_duration);
                time_ms_wait = time_ms_duration - time_ms_elapsed;
                LOGV("time ms elapsed: %ld.\n", time_ms_elapsed);
                LOGV("time ms to wait: %ld.\n", time_ms_wait);
                if (time_ms_wait > 10000) {
                    av_usleep(time_ms_wait);
                }
                if (time_ms_wait < -10000) {
                    *base_pts = packet.pts;
                    *base_time_ms = time_ms_current;
                }
            }
            LOGV("packet after decode pts=%ld dts=%ld.\n", packet.pts, packet.dts);
            sws_scale(sws_ctx, (const uint8_t *const *) decodedFrame->data,
                      decodedFrame->linesize, 0, codecCtx->height,
                      frameRGBA->data, frameRGBA->linesize
            );
            applyBitmapCallback();
        }
        return true;
    }

    ~CameraStreamHolder() {
        LOGI("destruct camera holder %s.\n", camera_source);
        if (!env->IsSameObject(callback, NULL)) {
            env->DeleteGlobalRef(callback);
        }
        if (buffer != NULL) {
            LOGI("unlock pixel buffer.\n");
            AndroidBitmap_unlockPixels(env, bitmap);
            buffer = NULL;
        }
        LOGI("pixel buffer is unlocked.\n");
        if (!env->IsSameObject(bitmap, NULL)) {
            LOGI("release bitmap instance.\n");
            env->DeleteGlobalRef(bitmap);
        }
        LOGI("bitmap instance is released.\n");
        if (sws_ctx != NULL) {
            LOGI("free sws context.\n")
            sws_freeContext(sws_ctx);
            sws_ctx = NULL;
        }
        LOGI("sws context is freed.\n");
        if (frameRGBA != NULL) {
            LOGI("free RGBA frame.\n");
            av_frame_free(&frameRGBA);
            frameRGBA = NULL;
        }
        LOGI("RGBA frame is freed.\n");
        if (decodedFrame != NULL) {
            LOGI("free decoded frame.\n");
            av_frame_free(&decodedFrame);
            decodedFrame = NULL;
        }
        LOGI("decoded frame is freed.\n");
        if (codecCtx != NULL) {
            LOGI("close codec context.\n");
            avcodec_close(codecCtx);
            decodedFrame = NULL;
        }
        LOGI("codec context is closed.\n");
        pCodec = NULL;
        videoStream = NULL;
        if (formatCtx != NULL) {
            LOGI("close format input.\n");
            avformat_close_input(&formatCtx);
            formatCtx = NULL;
        }
        LOGI("format input is closed.\n");
		if (options != NULL) {
            LOGI("free av options.\n");
			av_dict_free(&options);
			options = NULL;
		}
        LOGI("av options is freed.\n");
        if (camera_source != NULL) {
            LOGI("release camera source string.\n");
            env->ReleaseStringUTFChars(source, camera_source);
        }
        LOGI("camera source string is released.\n");
        int destLength = dests.size();
        for (int i = 0; i < destLength; i++) {
            ostreams[i] = NULL;
            ocodecCtxs[i] = NULL;
            if (!(oformatCtxs[i]->flags & AVFMT_NOFILE)) {
                LOGI("close avio: %d.\n", i);
                avio_close(oformatCtxs[i]->pb);
            }
            LOGI("free output context: %d.\n", i);
            avformat_free_context(oformatCtxs[i]);
            oformatCtxs[i] = NULL;
            LOGI("output context %d is freed.\n", i);
            env->ReleaseStringUTFChars(dests[i], camera_dests[i]);
            LOGI("output dest string %d is released.\n", i);
        }
        LOGI("deconstruction is done.\n");
    }
};


extern "C" JNIEXPORT jlong
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_decode(
        JNIEnv *env,
        jobject jobject1,
        jstring source,
        jobjectArray dests,
        jobject callback,
        jlong last_pts
) {
    CameraStreamHolder holder(env, source, dests, callback);
    if(!holder.init()) {
        return -1;
    }
    return holder.process_packets(last_pts);
}

extern "C" JNIEXPORT void
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_initJNI(
        JNIEnv *env,
        jclass jclass1
) {
    LOGI("FFmpeg initialization.\n");
	avdevice_register_all(); /* for device & add libavdevice/avdevice.h headerfile*/
    avcodec_register_all();
    av_register_all();
    avformat_network_init();
}
