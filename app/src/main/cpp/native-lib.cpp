#include <jni.h>
#include <string>
#include <vector>
#include <utility>
#include <android/log.h>
#include <android/bitmap.h>
#include <map>

using namespace std;

extern "C" {
#define __STDC_CONSTANT_MACROS
#include <sys/types.h>
#include <unistd.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/pixfmt.h>
#include <libavdevice/avdevice.h>
#include <libavutil/dict.h>
#include <libavutil/time.h>
#include <libavutil/mathematics.h>
}

#define LOG_TAG "android-ffmpeg"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__);
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__);
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);
#define VLOGV(fmt, ap) __android_log_vprint(ANDROID_LOG_VERBOSE, LOG_TAG, fmt, ap);
#define VLOGD(fmt, ap) __android_log_vprint(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ap);
#define VLOGI(fmt, ap) __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, fmt, ap);
#define VLOGW(fmt, ap) __android_log_vprint(ANDROID_LOG_WARN, LOG_TAG, fmt, ap);
#define VLOGE(fmt, ap) __android_log_vprint(ANDROID_LOG_ERROR, LOG_TAG, fmt, ap);

extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_configuration());
}

void custom_log(void *ptr, int level, const char* fmt, va_list vl){
    if (level == AV_LOG_FATAL || level == AV_LOG_ERROR) {
        VLOGE(fmt, vl);
    } else if (level == AV_LOG_WARNING) {
        VLOGW(fmt, vl);
    } else if (level == AV_LOG_INFO) {
        VLOGI(fmt, vl);
    } else if (level == AV_LOG_DEBUG) {
        VLOGD(fmt, vl);
    } else {
        VLOGV(fmt, vl);
    }
}

class CameraStreamHolder {
    jstring source;
	string camera_source;
    jobjectArray dests;
    jobject sourceProperties;
	vector<string> camera_dests;
    jobjectArray destsProperties;
    jobject fileDescriptors;
    map<string, string> camera_source_properties;
    vector<map<string, string> > camera_dests_properties;
    map<string, int> camera_file_descriptors;
    vector<AVFormatContext*> oformatCtxs;
    vector<AVCodecContext*> ocodecCtxs;
    vector<AVCodec*> ocodecs;
    vector<AVPacket*> opackets;
    vector<AVStream*> ostreams;
    vector<bool> encodes;
    vector<bool> destsStatus;

    int width = 0;
    int height = 0;
    jobject bitmapCallback = NULL;
    jobject finishCallback = NULL;
    jmethodID bitmapCallbackMethod = NULL;
    jmethodID finishCallbackMethod = NULL;
    jmethodID mapSetMethod = NULL;
    jmethodID setArrayMethod = NULL;
    jmethodID entryKey = NULL;
    jmethodID entryValue = NULL;
    jmethodID fdMethod = NULL;
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
    char* error_buf = NULL;

public:
    CameraStreamHolder(
            JNIEnv * const env, jstring source, jobjectArray dests,
            jobject bitmapCallback, jobject finishCallback,
            jobject sourceProperties, jobjectArray destsProperties,
            jobject fileDescriptors, jboolean sync
    ) : env(env), source(source), dests(dests),
        sourceProperties(sourceProperties), destsProperties(destsProperties),
        fileDescriptors(fileDescriptors), sync(sync) {
        if (!env->IsSameObject(bitmapCallback, NULL)) {
            this->bitmapCallback = env->NewGlobalRef(bitmapCallback);
            decode = true;
        }
        if (!env->IsSameObject(finishCallback, NULL)) {
            this->finishCallback = env->NewGlobalRef(finishCallback);
        }
        error_buf = (char*)av_malloc(4096);
    }

    bool getSource() {
        if (env->IsSameObject(source, NULL)) {
            LOGE("source is null.\n");
            return false;
        }
        const char* camera_source_str = env->GetStringUTFChars(source, NULL);
        camera_source = string(camera_source_str, strlen(camera_source_str));
        env->ReleaseStringUTFChars(source, camera_source_str);
        LOGI("create camera holder source: %s.\n", camera_source.c_str());
        return true;
    }

    bool getDests() {
        if (env->IsSameObject(dests, NULL)) {
            LOGI("dests is null.\n");
            return true;
        }
        int destLength = env->GetArrayLength(dests);
        if (env->ExceptionCheck()) {
            LOGE("Exception occurs to get array size.\n");
            return false;
        }
        for (int i = 0; i < destLength; i++) {
            jstring dest = (jstring)(env->GetObjectArrayElement(dests, i));
            if (env->ExceptionCheck()) {
                LOGE("Exception occurs to get array element %d.\n", i);
                return false;
            }
            if (env->IsSameObject(dest, NULL)) {
                LOGE("dest %d is null.\n", i);
                return false;
            }
            const char *camera_dest_str = env->GetStringUTFChars(dest, NULL);
            string camera_dest(camera_dest_str, strlen(camera_dest_str));
            env->ReleaseStringUTFChars(dest, camera_dest_str);
            LOGI(
                    "create camer dest %s for source %s.\n",
                    camera_dest.c_str(), camera_source.c_str()
            );
            camera_dests.push_back(camera_dest);
            oformatCtxs.push_back(NULL);
            ocodecCtxs.push_back(NULL);
            ocodecs.push_back(NULL);
            opackets.push_back(NULL);
            ostreams.push_back(NULL);
            encodes.push_back(false);
            destsStatus.push_back(true);
        }
        return true;
    }

    bool initMapConverter() {
        jclass mapClass = env->FindClass("java/util/Map");
        if (env->IsSameObject(mapClass, NULL)) {
            LOGE("failed to find map class");
            return false;
        }
        mapSetMethod = env->GetMethodID(mapClass, "entrySet", "()Ljava/util/Set;");
        if (mapSetMethod == NULL) {
            LOGE("failed to find entrySet method");
            return false;
        }
        jclass setClass = env->FindClass("java/util/Set");
        if (env->IsSameObject(setClass, NULL)) {
            LOGE("failed to find Set Class");
            return false;
        }
        setArrayMethod = env->GetMethodID(setClass, "toArray", "()[Ljava/lang/Object;");
        if (setArrayMethod == NULL) {
            LOGE("failed to find toArray method");
            return false;
        }
        jclass mapEntryClass =  env->FindClass("java/util/Map$Entry");
        if (env->IsSameObject(mapEntryClass, NULL)) {
            LOGE("failed to find Map.Entry class");
            return false;
        }
        entryKey = env->GetMethodID(mapEntryClass, "getKey", "()Ljava/lang/Object;");
        if (entryKey == NULL) {
            LOGE("failed to find getKey method");
            return false;
        }
        entryValue = env->GetMethodID(mapEntryClass, "getValue", "()Ljava/lang/Object;");
        if (entryValue == NULL) {
            LOGE("failed to find getValue method");
            return false;
        }
        return true;
    }

    bool initFileDescriptorConverter() {
        jclass fdClass = env->FindClass("android/os/ParcelFileDescriptor");
        if (env->IsSameObject(fdClass, NULL)) {
            LOGE("failed to get ParcelFileDescriptor.\n");
            return false;
        }
        fdMethod = env->GetMethodID(fdClass,"detachFd","()I");
        if (fdMethod == NULL) {
            LOGE("failed to get descriptor method.\n");
            return false;
        }
        return true;
    }

    bool convertMap(map<string, jobject>& converted_map, const jobject& orig_map) {
        if (env->IsSameObject(orig_map, NULL)) {
            LOGI("map object is null.\n");
            return true;
        }
        jobject mapSet = env->CallObjectMethod(orig_map, mapSetMethod);
        if (env->ExceptionCheck()) {
            LOGE("Exception occurs to get set.\n");
            return false;
        }
        if (env->IsSameObject(mapSet, NULL)) {
            LOGE("Map.entrySet is null.\n");
            return false;
        }
        jobject objArray = env->CallObjectMethod(mapSet, setArrayMethod);
        if (env->ExceptionCheck()) {
            LOGE("Exception occurs to get array.\n");
            return false;
        }
        if (env->IsSameObject(objArray, NULL)) {
            LOGE("array object is null.\n");
            return false;
        }
        jobjectArray array = reinterpret_cast<jobjectArray>(objArray);
        jsize len = env->GetArrayLength(array);
        if (env->ExceptionCheck()) {
            LOGE("Exception occurs to array size.\n");
            return false;
        }
        for (int i = 0; i < len; i++) {
            jobject entry = env->GetObjectArrayElement(array, i);
            if (env->ExceptionCheck()) {
                LOGE("Exception occurs to array element %d.\n", i);
                return false;
            }
            if (env->IsSameObject(entry, NULL)) {
                LOGE("failed to get entry at %d", i);
                return false;
            }
            jstring key = reinterpret_cast<jstring>(env->CallObjectMethod(entry, entryKey));
            if (env->ExceptionCheck()) {
                LOGE("Exception occurs to get key at array element %d.\n", i);
                return false;
            }
            if (env->IsSameObject(key, NULL)) {
                LOGE("failed to get key at %d.\n", i);
                return false;
            }
            jobject value =  env->CallObjectMethod(entry, entryValue);
            if (env->ExceptionCheck()) {
                LOGE("Exception occurs to get value at array element %d.\n", i);
                return false;
            }
            if (env->IsSameObject(value, NULL)) {
                LOGE("failed to get value at %d.\n", i);
                return false;
            }
            const char* keyStr = env->GetStringUTFChars(key, NULL);
            converted_map[string(keyStr, strlen(keyStr))] = value;
            env->ReleaseStringUTFChars(key, keyStr);
        }
        return true;
    }

    bool convertStringMap(map<string, string>& converted_map, const jobject& orig_map) {
        map<string, jobject> object_map;
        if (!convertMap(object_map, orig_map)) {
            LOGE("failed to convert map.\n");
            return false;
        }
        for (map<string, jobject>::iterator it = object_map.begin(); it != object_map.end(); ++it) {
            const string& key = it->first;
            jstring value = reinterpret_cast<jstring>(it->second);
            const char* valueString = env->GetStringUTFChars(value, NULL);
            converted_map[key] = string(valueString, strlen(valueString));
            env->ReleaseStringUTFChars(value, valueString);
            LOGI("get map key=%s value=%s.\n", key.c_str(), valueString);
        }
        return true;
    }

    bool getSourceProperties() {
        return convertStringMap(camera_source_properties, sourceProperties);
    }

    bool getFileDescriptors() {
        map<string, jobject> convert_map;
        if (!convertMap(convert_map, fileDescriptors)) {
            LOGE("failed to convert file descriptors map.\n");
            return false;
        }
        for (
                map<string, jobject>::iterator it =  convert_map.begin();
                it != convert_map.end(); ++it
        ) {
            const string& key = it->first;
            jobject value = it->second;
            int fd = env->CallIntMethod(value, fdMethod);
            if (env->ExceptionCheck()) {
                LOGE("failed to get fd.\n");
                return false;
            }
            int offset = lseek(fd, 0, SEEK_CUR);
            LOGI("file descriptor %d current position: %d", fd, offset);
            camera_file_descriptors[key] = fd;
            LOGI("get file descriptor %d from %s.\n", fd, key.c_str());
        }
        return true;
    }

    bool getDestsProperties() {
        if (env->IsSameObject(destsProperties, NULL)) {
            LOGI("dests properties is null.\n");
            return true;
        }
        int destLength = env->GetArrayLength(destsProperties);
        if (env->ExceptionCheck()) {
            LOGE("Exception occurs to get array size.\n");
            return false;
        }
        if (destLength != camera_dests.size()) {
            LOGE(
                    "dests properties size %d does not match camera dests %d.\n",
                    destLength, (int)camera_dests.size()
            );
            return false;
        }
        for (int i = 0; i < destLength; i++) {
            jobject dest_properties = env->GetObjectArrayElement(destsProperties, i);
            if (env->ExceptionCheck()) {
                LOGE("Exception occurs to get array element %d.\n", i);
                return false;
            }
            map<string, string> camera_dest_properties;
            if (env->IsSameObject(dest_properties, NULL)) {
                LOGI("dest %d properties is null", i);
            } else {
                if (!convertStringMap(camera_dest_properties, dest_properties)) {
                    LOGE("failed to convert string map.\n");
                    return false;
                }
            }
            camera_dests_properties.push_back(camera_dest_properties);
        }
        return true;
    }

    bool initBitmapCallbackMethod() {
        LOGI("init bitmapCallback method.\n");
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/BitmapCallback");
        if (env->IsSameObject(cbClass, NULL)) {
            LOGE("failed to get BitmapCallback class.\n");
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
        LOGV("apply bitmap callback on %s.\n", camera_source.c_str());
        if (env->IsSameObject(bitmapCallback, NULL)) {
            LOGV("callback is NULL.\n");
            return;
        }
        env->CallVoidMethod(bitmapCallback, bitmapCallbackMethod, bitmap);
        if (env->ExceptionCheck()) {
            LOGE("Exception occurs when call bitmapCallback.\n");
        }
    }

    bool initFinishCallbackMethod() {
        LOGI("init finish callback method.\n");
        jclass cbClass = env->FindClass("com/example/xiaodong/testvideo/FinishCallback");
        if (env->IsSameObject(cbClass, NULL)) {
            LOGE("failed to get FinishCallback class.\n");
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
        LOGV("apply finish callback on %s.\n", camera_source.c_str());
        if (env->IsSameObject(finishCallback, NULL)) {
            LOGV("callback is NULL");
            return true;
        }
        jboolean finished = env->CallBooleanMethod(finishCallback, finishCallbackMethod);
        if (env->ExceptionCheck()) {
            LOGE("exception occurs when calling finishCallback.\n");
            return true;
        }
        LOGV("should be finished? %d.\n", finished);
        return finished;
    }

    bool createBitmap() {
        LOGI("create bitmap.\n");
        //get Bitmap class and createBitmap method ID
        jclass javaBitmapClass = (jclass)env->FindClass("android/graphics/Bitmap");
        if (env->IsSameObject(javaBitmapClass, NULL)) {
            LOGE("Failed to get Bitmap Class.\n");
            return false;
        }
        jmethodID mid = env->GetStaticMethodID(
                javaBitmapClass, "createBitmap",
                "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"
        );
        if (mid == NULL) {
            LOGE("failed to get CreateBitmap method.\n");
            return false;
        }
        //create Bitmap.Config
        //reference: https://forums.oracle.com/thread/1548728
        jstring jConfigName = env->NewStringUTF("ARGB_8888");
        jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
        if (env->IsSameObject(bitmapConfigClass, NULL)) {
            LOGE("failed to get Bitmap$Config class.\n");
            return false;
        }
        jobject javaBitmapConfig = env->CallStaticObjectMethod(
                bitmapConfigClass,
                env->GetStaticMethodID(
                        bitmapConfigClass, "valueOf",
                        "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;"
                ),
                jConfigName
        );
        if (env->ExceptionCheck()) {
            LOGE("Exception raises to create BitmapConfig.\n");
            return false;
        }
        if (env->IsSameObject(javaBitmapConfig, NULL)) {
            LOGE("failed to get Bitmap$Config instance.\n");
            return false;
        }
        //create the bitmap
        jobject localBitmap = env->CallStaticObjectMethod(
                javaBitmapClass, mid, width, height, javaBitmapConfig
        );
        if (env->ExceptionCheck()) {
            LOGE("exception raises to create bitmap.\n");
            return false;
        }
        if (env->IsSameObject(localBitmap, NULL)) {
            LOGE("failed to get Bitmap instance.\n");
            return false;
        }
        bitmap = reinterpret_cast<jobject>(env->NewGlobalRef(localBitmap));
        return true;
    }

    void copy_codec_info(AVCodecContext* ocodec, AVCodecContext* icodec) {
        ocodec->bit_rate = icodec->bit_rate;
    }

    void copy_stream_info(AVStream* ostream, AVStream* istream){
        ostream->id = istream->id;
        // avcodec_parameters_copy(ostream->codecpar, istream->codecpar);
    }

    void copy_video_codec_info(
            AVCodecContext* ocodec, AVCodecContext* icodec
    ) {
        copy_codec_info(ocodec, icodec);
        ocodec->width = icodec->width;
        ocodec->height = icodec->height;
        ocodec->time_base = icodec->time_base;
        ocodec->framerate = icodec->framerate;
        // ocodec->max_b_frames = icodec->max_b_frames;
        ocodec->gop_size = icodec->gop_size;
        ocodec->sample_aspect_ratio = icodec->sample_aspect_ratio;
        // ocodec->pix_fmt = icodec->pix_fmt;
    }

    void copy_audio_codec_info(AVCodecContext* ocodec, AVCodecContext* icodec) {
        copy_codec_info(ocodec, icodec);
        // ocodec->sample_fmt = icodec->sample_fmt;
        ocodec->sample_rate = icodec->sample_rate;
        ocodec->channel_layout = icodec->channel_layout;
        ocodec->channels = icodec->channels;
        ocodec->time_base = icodec->time_base;
    }

    void copy_video_stream_info(AVStream* ostream, AVStream* istream){
        copy_stream_info(ostream, istream);
        ostream->time_base = istream->time_base;
        ostream->r_frame_rate = istream->r_frame_rate;
        ostream->avg_frame_rate = istream->avg_frame_rate;
    }

    void copy_audio_stream_info(AVStream* ostream, AVStream* istream){
        copy_stream_info(ostream, istream);
    }

    void check_error(int err_code) {
        if (error_buf != NULL) {
            av_strerror(err_code, error_buf, 4096);
            LOGE("error code %d: %s.\n", err_code, error_buf);
        } else {
            LOGE("failed to alloc buf to print err message.\n");
        }
    }

    bool init() {
        if (initialized) {
            LOGI("is already initialized.\n")
            return true;
        }
        LOGI("initialize map converter.\n");
        if (!initMapConverter()) {
            LOGE("failed to initialize map converter.\n");
            return false;
        }
        LOGI("initialize file descriptor field.\n");
        if (!initFileDescriptorConverter()) {
            LOGE("failed to initialize file descriptor field.\n");
            return false;
        }
        LOGI("file descriptor field is got.\n");
        if (!initBitmapCallbackMethod()) {
            LOGE("failed to init bitmap callback method.\n");
            return false;
        }
        LOGI("bitmap callback methed is got.\n");
        if (!initFinishCallbackMethod()) {
            LOGE("failed to init finish callback method.\n");
            return false;
        }
        LOGI("finish callback method is got.\n");
        if (!getSource()) {
            LOGE("failed to setup camera source.\n");
            return false;
        }
        LOGI("camera source is got: %s.\n", camera_source.c_str());
        if (!getDests()) {
            LOGE("failed to setup camera dests.\n");
            return false;
        }
        LOGI("camera dests are got.\n");
        if (!getSourceProperties()) {
            LOGE("failed to setup source properties.\n");
            return false;
        }
        for (
                map<string, string>::const_iterator it = camera_source_properties.begin();
                it != camera_source_properties.end();
                ++it
        ) {
            LOGI(
                    "camera source %s find property %s=%s.\n",
                    camera_source.c_str(),
                    it->first.c_str(), it->second.c_str()
            );
        }
        LOGI("camera source properties are got.\n");
        if (!getDestsProperties()) {
            LOGE("failed to setup dests properties.\n");
            return false;
        }
        for (
                int i = 0; i < camera_dests_properties.size(); ++i
        ) {
            for (
                    map<string, string>::const_iterator it = camera_dests_properties[i].begin();
                    it != camera_dests_properties[i].end();
                    ++it
            ) {
                LOGI(
                        "camera dest %s find property %s=%s.\n",
                        camera_dests[i].c_str(), it->first.c_str(), it->second.c_str()
                );
            }
        }
        LOGI("camera dests properties are got.\n");
        if (!getFileDescriptors()) {
            LOGE("failed to set file descriptors.\n");
        }
        LOGI("file descriptors are got.\n");
        //FFmpeg av_log() callback
        av_log_set_callback(custom_log);
        if (camera_source_properties.find("input_format") != camera_source_properties.end()) {
            inputFormat = av_find_input_format(camera_source_properties["input_format"].c_str());
            LOGI("set input format %s.\n", inputFormat->name);
        }
        if (camera_file_descriptors.find(camera_source) != camera_file_descriptors.end()) {
            char path[20];
            int fd = camera_file_descriptors[camera_source];
            sprintf(path, "pipe:%d", fd);
            camera_source = string(path, strlen(path));
            LOGI("reset camera source to %s.\n", camera_source.c_str());
        }
        int err_code;
        if ((err_code = avformat_open_input(&formatCtx, camera_source.c_str(), inputFormat,
                                            &options)) < 0) {
            LOGE("Couldn't open input %s.\n", camera_source.c_str());
            check_error(err_code);
            return false;
        }
        for (int i = 0; i < formatCtx->nb_streams; ++i) {
            LOGI("stream %d codec %p", i, formatCtx->streams[i]->codec);
        }
        LOGI("camera input %s is opened.\n", camera_source.c_str());
        // Retrieve stream information
        if ((err_code = avformat_find_stream_info(formatCtx, NULL)) < 0) {
            LOGE("FAILED to find stream info %s.\n", camera_source.c_str());
            check_error(err_code);
            return false; // Couldn't find stream information
        }
        LOGI("read camera %s stream info success.\n", camera_source.c_str());
        // Dump information about file onto standard error
        av_dump_format(formatCtx, 0, camera_source.c_str(), 0);
        for (int i = 0; i < formatCtx->nb_streams; i++) {
            LOGI("got stream %d codec type %d", i, formatCtx->streams[i]->codec->codec_type);
        }
        err_code = av_find_best_stream(formatCtx, AVMEDIA_TYPE_VIDEO, -1, -1, &pCodec, 0);
        if (err_code < 0) {
            LOGE("Didn't find a video stream.\n");
            check_error(err_code);
            return false; // Didn't find a video stream
        }
        if(pCodec == NULL) {
            LOGE("Unsupported codec.\n");
            return false; // Codec not found
        }
        LOGI("get codec success: %s.\n", pCodec->name);
        videoStreamIndex = err_code;
        videoStream = formatCtx->streams[videoStreamIndex];
        if (videoStream == NULL) {
            LOGE("failed to get video stream.\n");
            return false;
        }
        LOGI("find video stream %d.\n", videoStreamIndex);
        if (videoStream->codec->pix_fmt == AV_PIX_FMT_NONE) {
            LOGE("%s no proper pix fmt found in codec context.\n", camera_source.c_str());
            return false;
        }
        // Get a pointer to the codec context for the video stream
        // codecCtx = videoStream->codec;
        // Copy context
        codecCtx = avcodec_alloc_context3(pCodec);
        if(avcodec_copy_context(codecCtx, videoStream->codec) != 0) {
            LOGE("Couldn't copy codec context.\n");
            return false; // Error copying codec context
        }
        LOGI("get codec context success.\n");
        // Open codec
        if((err_code = avcodec_open2(codecCtx, pCodec, NULL)) < 0) {
            LOGE("Could not open codec.\n");
            check_error(err_code);
            return false; // Could not open codec
        }
        LOGI("open codec success.\n");
        if (codecCtx->pix_fmt == AV_PIX_FMT_NONE) {
            LOGE("failed to get codec context pix format.\n");
            return false;
        }
        width = codecCtx->width;
        LOGI("get width %d.\n", width);
        height = codecCtx->height;
        LOGI("get height %d.\n", height);
        LOGI("pix_fmt=%d.\n", codecCtx->pix_fmt);
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
        if (!createBitmap()) {
            LOGE("failed to create bitmap.\n");
            return false;
        }
        LOGI("bitmap instance is created.\n");
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
        int destLength = camera_dests.size();
        for (int i = 0; i < destLength; i++) {
            string& camera_dest = camera_dests[i];
            map<string, string>& camera_dest_properties = camera_dests_properties[i];
            const char* outputFormat = NULL;
            if (camera_dest_properties.find("output_format") != camera_dest_properties.end()) {
                outputFormat = camera_dest_properties["output_format"].c_str();
                LOGI("set output format %s.\n", outputFormat);
            }
            LOGI("%s expected output format is %s.\n", camera_dest.c_str(), outputFormat);
            if((err_code = avformat_alloc_output_context2(
                    &oformatCtxs[i], NULL, outputFormat, camera_dest.c_str())
               ) < 0) {
                LOGE("oformatCtx %s is failed to alloc.\n", camera_dest.c_str());
                check_error(err_code);
                return false;
            }
            LOGI("alloc output context for %s.\n", camera_dest.c_str());
            AVFormatContext* oformatCtx = oformatCtxs[i];
            if (!(oformatCtx->flags & AVFMT_NOFILE)) {
                if ((err_code = avio_open2(
                        &(oformatCtx->pb), camera_dest.c_str(),
                        AVIO_FLAG_WRITE, &(oformatCtx->interrupt_callback), NULL
                )) < 0) {
                    LOGE("failed to open AVIO: %s.\n", camera_dest.c_str());
                    check_error(err_code);
                    return false;
                }
                if (oformatCtx->pb == NULL) {
                    LOGE("failed to alloc pb: %s.\n", camera_dest.c_str());
                    return false;
                }
            } else {
                LOGI("No need to open avio: %s.\n", camera_dest.c_str());
            }
            LOGI("open avio for %s success.\n", camera_dest.c_str());
            AVCodec* outputCodec = NULL;
            const char* outputCodecName = NULL;
            if (camera_dest_properties.find("output_codec") != camera_dest_properties.end()) {
                outputCodecName = camera_dest_properties["output_codec"].c_str();
                LOGI("set output codec %s.\n", outputCodecName);
            }
            if (outputCodecName != NULL) {
                outputCodec = avcodec_find_encoder_by_name(outputCodecName);
                if (outputCodec == NULL) {
                    LOGE(
                            "failed to find output %s codec by name %s.\n",
                            camera_dest.c_str(), outputCodecName
                    );
                    return false;
                }
            } else {
                outputCodec = pCodec;
            }
            if (outputCodec->id != pCodec->id) {
                LOGI("output %s needs to decode then encode.\n", camera_dest.c_str());
                encodes[i] = true;
                decode = true;
            }
            ocodecs[i] = outputCodec;
            LOGI("use codec %s to encoding %s.\n", outputCodec->name, camera_dest.c_str());
            AVStream* ostream = avformat_new_stream(oformatCtx, outputCodec);
            if (ostream == NULL) {
                LOGE("failed to create stream: %s.\n", camera_dest.c_str());
                return false;
            }
            LOGI("create new stream for %s.\n", camera_dest.c_str());
            ostreams[i] = ostream;
            AVCodecContext* ocodecCtx = NULL;
            if (encodes[i]) {
                ocodecCtx = avcodec_alloc_context3(outputCodec);
                if (ocodecCtx == NULL) {
                    LOGE("failed to get output codec context for %s.\n", camera_dest.c_str());
                    return false;
                }
                // ostream->id = oformatCtx->nb_streams - 1;
                if (outputCodec->pix_fmts != NULL) {
                    i = 0;
                    while (outputCodec->pix_fmts[i] != AV_PIX_FMT_NONE) {
                        LOGI(
                                "%s found support pix fmt %d.\n",
                                camera_dest.c_str(), outputCodec->pix_fmts[i]
                        );
                        ocodecCtx->pix_fmt = outputCodec->pix_fmts[i];
                        ++i;
                    }
                } else {
                    LOGE("%s support pix fmts is null.\n", camera_dest.c_str());
                    return false;
                }
                if((err_code = avcodec_parameters_to_context(ocodecCtx, ostream->codecpar)) < 0) {
                    LOGE(
                            "failed to copy stream codecpar to codec context %s.\n",
                            camera_dest.c_str()
                    );
                    check_error(err_code);
                    return false;
                }
            } else {
                ocodecCtx = ostream->codec;
                if ((err_code = avcodec_copy_context(ocodecCtx, codecCtx)) < 0) {
                    LOGE("failed to copy codec context to %s.\n", camera_dest.c_str());
                    check_error(err_code);
                    return false;
                }
                // ocodecCtx->codec_id = codecCtx->codec_id;
                // ocodecCtx->codec_type = codecCtx->codec_type;
                int extra_size = (uint64_t)codecCtx->extradata_size + AV_INPUT_BUFFER_PADDING_SIZE;
                ocodecCtx->extradata = (uint8_t*)av_mallocz(extra_size);
                memcpy(ocodecCtx->extradata, codecCtx->extradata, codecCtx->extradata_size);
                ocodecCtx->extradata_size = codecCtx->extradata_size;
                ocodecCtx->codec_tag = 0;
                ocodecCtx->pix_fmt = codecCtx->pix_fmt;
            }
            ocodecCtxs[i] = ocodecCtx;
            // Some formats want stream headers to be separate.
            if (oformatCtx->oformat->flags & AVFMT_GLOBALHEADER){
                ocodecCtx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
            }
            LOGI("open oput stream for %s success.\n", camera_dest.c_str());
            copy_video_codec_info(ocodecCtx, codecCtx);
            // copy_video_stream_info(ostream, videoStream);
            if (encodes[i]) {
                if ((err_code = avcodec_open2(ocodecCtx, outputCodec, NULL)) < 0) {
                    LOGE("failed to open codec to %s", camera_dest.c_str());
                    check_error(err_code);
                    return false;
                }
            }
            av_dump_format(oformatCtx, 0, camera_dest.c_str(), 1);
            AVPacket* opacket = av_packet_alloc();
            if (opacket == NULL) {
                LOGE("failed to alloc packet to %s.\n", camera_dest.c_str());
                return false;
            }
            av_init_packet(opacket);
            opackets[i] = opacket;
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
        LOGI("process packets with decode=%d sync=%d.\n", decode, sync)
        AVRational ms_rational = av_make_q(1, AV_TIME_BASE);
        AVPacket packet;
        av_init_packet(&packet);
        int64_t base_time_ms = av_gettime();
        int64_t base_pts = last_pts;
        int64_t callback_per_frames = 0;
        if (camera_source_properties.find("callback_per_frames") != camera_source_properties.end()) {
            callback_per_frames = atol(camera_source_properties["callback_per_frames"].c_str());
        }
        int64_t callback_per_duration = 0;
        if (camera_source_properties.find("callback_per_duration") != camera_source_properties.end()) {
            callback_per_duration = atol(camera_source_properties["callback_per_duration"].c_str());
        }
        int err_code;
        int destLength = camera_dests.size();
        for (int i = 0; i < destLength; i++) {
            string& camera_dest = camera_dests[i];
            AVFormatContext *oformatCtx = oformatCtxs[i];
            if ((err_code = avformat_write_header(oformatCtx, NULL)) < 0) {
                LOGE("failed to write header: %s.\n", camera_dest.c_str());
                check_error(err_code);
				destsStatus[i] = false;
                return last_pts;
            } else {
                LOGI("write header to %s.\n", camera_dest.c_str());
            }
        }
        if ((err_code = av_seek_frame(formatCtx, videoStreamIndex, last_pts, 0)) < 0) {
            LOGE("failed to seek frame at %ld.\n", last_pts);
            check_error(err_code);
			last_pts = 0;
        }
		int frame_index = 0;
        int64_t decoded_frames = 0;
        int64_t callback_frames = 0;
        int64_t callback_duration = 0;
        av_init_packet(&packet);
        bool status = true;
        while (status) {
            if ((err_code = av_read_frame(formatCtx, &packet)) < 0) {
                LOGE("Failed to read frame.\n");
                check_error(err_code);
                break;
            }
            if(packet.stream_index == videoStreamIndex){
                if(packet.pts == AV_NOPTS_VALUE){
					//Write PTS  
					AVRational time_base1 = videoStream->time_base;  
					//Duration between 2 frames (us)  
					int64_t calc_duration = av_rescale(
                            AV_TIME_BASE,
                            videoStream->r_frame_rate.den,
                            videoStream->r_frame_rate.num
                    );
					//Parameters  
					packet.pts = av_rescale_q(frame_index * calc_duration, ms_rational, time_base1);
					packet.dts = packet.pts;  
					packet.duration = av_rescale_q(calc_duration, ms_rational, time_base1);
				}
				frame_index++;
				LOGV(
                        "packet %d demux pts=%ld dts=%ld duration=%ld.\n",
                        codecCtx->frame_number, packet.pts, packet.dts, packet.duration
                );
                last_pts = packet.pts;
                if (decode) {
                    LOGV(
                            "decode one packet at base time ms=%ld base pts=%ld.\n",
                            base_time_ms, base_pts
                    )
                    if(!decode_packet(
                            packet, &base_time_ms, &base_pts,
                            &decoded_frames,
                            &callback_frames, &callback_duration,
                            callback_per_frames, callback_per_duration
                    )) {
                        LOGE(
                                "failed to decode packet at base time=%ld base pts=%ld.\n",
                                base_time_ms, base_pts
                        );
                        status = false;
                    }
                }
                if (status) {
                    status = copy_packets(packet);
                }
            } else {
                LOGV("ignore stream %d.\n", packet.stream_index)
            }
            av_packet_unref(&packet);
            // packet = AVPacket();
            if(applyFinishCallback()) {
                LOGI("frame parsing is finished early.\n");
                break;
            }
        }
        for (int i = 0; i < destLength; i++) {
            string& camera_dest = camera_dests[i];
            AVFormatContext* oformatCtx = oformatCtxs[i];
			if (destsStatus[i]) {
				int err_code;
				if((err_code = av_write_trailer(oformatCtx)) < 0) {
					LOGE("failed to write trailer: %s.\n", camera_dest.c_str());
					check_error(err_code);
					destsStatus[i] = false;
				} else {
					LOGI("write trailer: %s.\n", camera_dest.c_str());
				}
			}
        }
        return last_pts;
    }

    bool copy_packets(
            AVPacket& packet
    ) {
        int destLength = camera_dests.size();
        bool status = true;
        for (int i = 0; i < destLength; i++) {
            string& camera_dest = camera_dests[i];
            AVFormatContext* oformatCtx = oformatCtxs[i];
            AVCodecContext* ocodecCtx = ocodecCtxs[i];
            AVStream* ostream = ostreams[i];
            bool encode = encodes[i];
            if (!encode) {
                if (destsStatus[i]) {
                    AVPacket *opacket = opackets[i];
                    destsStatus[i] = copy_packet(
                            opacket, packet, oformatCtx, ocodecCtx,
                            ostream, camera_dest
                    );
                } else {
                    LOGD(
                            "ignore copy packet to %s since status is already false.\n",
                            camera_dest.c_str()
                    );
                }
                if (!destsStatus[i]) {
                    status = false;
                }
            } else {
                LOGD(
                        "ignore copy packet to %s since it needs encode/\n",
                        camera_dest.c_str()
                );
            }
        }
        return status;
    }

    bool copy_packet(
            AVPacket* opacket,
            AVPacket& packet,
            AVFormatContext* oformatCtx,
            AVCodecContext* ocodecCtx,
            AVStream* ostream,
            const string& camera_dest
    ) {
        int err_code;
        int status = true;
        if ((err_code = av_packet_ref(opacket, &packet)) < 0) {
            LOGE("failed to get output packet: %s.\n", camera_dest.c_str());
            check_error(err_code);
            return false;
        }
        opacket->pts = av_rescale_q_rnd(
                packet.pts, videoStream->time_base, ostream->time_base,
                AVRounding(AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)
        );
        opacket->dts = av_rescale_q_rnd(
                packet.dts, videoStream->time_base, ostream->time_base,
                AVRounding(AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX)
        );
        opacket->duration = av_rescale_q(
                packet.duration, videoStream->time_base, ostream->time_base
        );
        LOGV(
                "packet %d mux pts=%ld dts=%ld duration=%ld.\n",
                ocodecCtx->frame_number, opacket->pts, opacket->dts, opacket->duration
        );
        if ((err_code = av_interleaved_write_frame(oformatCtx, opacket)) < 0) {
            LOGE("failed to write frame: %s.\n", camera_dest.c_str());
            check_error(err_code);
            status = false;
        } else {
            ocodecCtx->frame_number++;
            LOGV("write one frame: %s.\n", camera_dest.c_str());
        }
        av_packet_unref(opacket);
        return status;
    }

    bool encode_packets(AVPacket& packet) {
        int destLength = camera_dests.size();
        bool status = true;
        for (int i = 0; i < destLength; i++) {
            string& camera_dest = camera_dests[i];
            AVFormatContext* oformatCtx = oformatCtxs[i];
            AVCodecContext* ocodecCtx = ocodecCtxs[i];
            AVStream* ostream = ostreams[i];
            bool encode = encodes[i];
            if (encode) {
                if (destsStatus[i]) {
                    AVPacket *opacket = opackets[i];
                    destsStatus[i] = encode_packet(
                            opacket, packet, oformatCtx, ocodecCtx,
                            ostream, camera_dest
                    );
                } else {
                    LOGD(
                            "ignore encode packet to %s since status is already false.\n",
                            camera_dest.c_str()
                    );
                }
                if (!destsStatus[i]) {
                    status = false;
                }
            } else {
                LOGD(
                        "ignore encode packet to %s since it does not need encode.\n",
                        camera_dest.c_str()
                );
            }
        }
        return status;
    }

    void sync_frame(
            int64_t* base_pts, int64_t* base_time_ms,
            int current_pts,
            int64_t base_duration_ms, int64_t duration_ms_current
    ) {
        if (sync) {
            int64_t time_ms_wait = 0;
            int64_t time_ms_current = av_gettime();
            int64_t time_ms_elapsed = time_ms_current - *base_time_ms;
            int64_t time_ms_duration = duration_ms_current - base_duration_ms;
            LOGV("time duration: %ld.\n", time_ms_duration);
            time_ms_wait = time_ms_duration - time_ms_elapsed;
            LOGV("time ms elapsed: %ld.\n", time_ms_elapsed);
            LOGV("time ms to wait: %ld.\n", time_ms_wait);
            if (time_ms_wait > 10000) {
                av_usleep(time_ms_wait);
            }
            if (time_ms_wait < -10000) {
                *base_pts = current_pts;
                *base_time_ms = time_ms_current;
            }
        }
    }

    bool encode_packet(
            AVPacket* opacket,
            AVPacket& packet,
            AVFormatContext* oformatCtx,
            AVCodecContext* ocodecCtx,
            AVStream* ostream,
            const string& camera_dest
    ) {
        int err_code;
        bool status = true;
        if ((err_code = avcodec_is_open(ocodecCtx)) < 0) {
            LOGE("%s codec context is not opened.\n", camera_dest.c_str());
            check_error(err_code);
            return false;
        }
        const AVCodec* codec = ocodecCtx->codec;
        if ((err_code = av_codec_is_encoder(codec)) < 0) {

            LOGE("%s codec %s is not encoder.\n", camera_dest.c_str(), codec->name);
            check_error(err_code);
            return false;
        }
        if ((err_code = avcodec_send_frame(ocodecCtx, decodedFrame)) < 0) {
            LOGE("failed to encode frame %s.\n", camera_dest.c_str());
            check_error(err_code);
            return false;
        }
        av_init_packet(opacket);
        while (status) {
            if ((err_code = avcodec_receive_packet(ocodecCtx, opacket)) < 0) {
                if (err_code != AVERROR(EAGAIN) && err_code != AVERROR_EOF) {
                    LOGE("failed to receive packet.\n");
                    check_error(err_code);
                    return false;
                }
                break;
            }
            LOGV("receive one packet.\n");
            opacket->pts = av_rescale_q_rnd(
                    packet.pts, videoStream->time_base, ostream->time_base,
                    AVRounding(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX)
            );
            opacket->dts = av_rescale_q_rnd(
                    packet.dts, videoStream->time_base, ostream->time_base,
                    AVRounding(AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX)
            );
            opacket->duration = av_rescale_q(
                    packet.duration, videoStream->time_base, ostream->time_base
            );
            LOGV(
                    "packet %d mux pts=%ld dts=%ld duration=%ld.\n",
                    ocodecCtx->frame_number, opacket->pts, opacket->dts, opacket->duration
            );
            if((err_code = av_interleaved_write_frame(oformatCtx, opacket)) < 0) {
                LOGE("failed to write frame: %s.\n", camera_dest.c_str());
                check_error(err_code);
                status = false;
            } else {
                LOGV("write one frame: %s.\n", camera_dest.c_str());
                ocodecCtx->frame_number++;
            }
            av_packet_unref(opacket);
        }
        return status;
    }

    bool decode_packet(
            AVPacket& packet,
            int64_t* base_time_ms, int64_t* base_pts,
            int64_t* decoded_frames,
            int64_t* callback_frames,
            int64_t* callback_duration,
            int64_t callback_per_frames,
            int64_t callback_per_duration
    ) {
        LOGV(
                "decode one packet at base time ms=%ld base pts=%ld.\n",
                *base_time_ms, *base_pts
        )
        AVRational ms_rational = av_make_q(1, AV_TIME_BASE);
        int err_code = 0;
        if ((err_code = avcodec_send_packet(codecCtx, &packet)) < 0) {
            if (err_code != AVERROR(EAGAIN) && err_code != AVERROR_EOF) {
                check_error(err_code);
                LOGE("failed to send packet.\n");
                return false;
            }
            LOGV("finish decode since it buffer cleans.\n");
            return true;
        }
        LOGV("send one packet.\n");
        int64_t base_duration_ms = av_rescale_q(
                *base_pts, videoStream->time_base, ms_rational
        );
        while (true) {
            if ((err_code = avcodec_receive_frame(
                    codecCtx, decodedFrame
            )) < 0) {
                if (err_code != AVERROR(EAGAIN) && err_code != AVERROR_EOF) {
                    LOGE("failed to receive frame.\n");
                    check_error(err_code);
                    return false;
                }
                break;
            }
            LOGV("receive one frame.\n");
            bool status = encode_packets(packet);
            if (!status) {
                return false;
            }
            *decoded_frames++;
            int64_t duration_ms_current = av_rescale_q(
                    packet.pts, videoStream->time_base, ms_rational
            );
            sync_frame(
                    base_pts, base_time_ms, packet.pts,
                    base_duration_ms, duration_ms_current
            );
            if (callback_per_frames > 0) {
                if (*decoded_frames - *callback_frames < callback_per_frames) {
                    LOGV("ignore %ld frame while callback frame is %ld.\n", decoded_frames,
                         callback_frames);
                    continue;
                } else {
                    *callback_frames = *decoded_frames;
                }
            }
            if (callback_per_duration > 0) {

                if (duration_ms_current - *callback_duration < callback_per_duration) {
                    LOGV("ignore %ld duration while callback duration is %ld.\n",
                         duration_ms_current, *callback_duration);
                    continue;
                }
                else {
                    *callback_duration = duration_ms_current;
                }
            }
            sws_scale(sws_ctx, (const uint8_t *const *) decodedFrame->data,
                      decodedFrame->linesize, 0, codecCtx->height,
                      frameRGBA->data, frameRGBA->linesize
            );
            applyBitmapCallback();
        }
        LOGV(
                "packet after decode pts=%ld dts=%ld duration=%ld.\n",
                packet.pts, packet.dts, packet.duration
        );
        return true;
    }

    ~CameraStreamHolder() {
        LOGI("destruct camera holder %s.\n", camera_source.c_str());
        if (!env->IsSameObject(bitmapCallback, NULL)) {
            env->DeleteGlobalRef(bitmapCallback);
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
            codecCtx = NULL;
        }
        LOGI("codec context is closed.\n");
        pCodec = NULL;
        videoStream = NULL;
        inputFormat = NULL;
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
        int destLength = camera_dests.size();
        for (int i = 0; i < destLength; i++) {
            if (opackets[i] != NULL) {
                av_packet_free(&opackets[i]);
            }
            opackets[i] = NULL;
            ostreams[i] = NULL;
			if (ocodecCtxs[i] != NULL) {
				avcodec_close(ocodecCtxs[i]);
			}
            ocodecCtxs[i] = NULL;
            ocodecs[i] = NULL;
			if (oformatCtxs[i] != NULL) {
				if (!(oformatCtxs[i]->flags & AVFMT_NOFILE)) {
					LOGI("close avio: %d.\n", i);
					avio_close(oformatCtxs[i]->pb);
				}
				LOGI("free output context: %d.\n", i);
				avformat_free_context(oformatCtxs[i]);
			}
            oformatCtxs[i] = NULL;
            LOGI("output context %d is freed.\n", i);
        }
        for (
                map<string, int>::const_iterator it = camera_file_descriptors.begin();
                it != camera_file_descriptors.end(); ++it
        ) {
            int fd = it->second;
            close(fd);
        }
        av_free(error_buf);
        error_buf = NULL;
        LOGI("deconstruction is done.\n");
    }
};


extern "C" JNIEXPORT jboolean
JNICALL
Java_com_example_xiaodong_testvideo_FFmpeg_decode(
        JNIEnv *env,
        jobject jobject1,
        jstring source,
        jobjectArray dests,
        jobject bitmapCallback,
        jobject finishCallback,
        jobject sourceProperties,
        jobjectArray destsProperties,
        jobject fileDescriptors,
        jlong last_pts=0,
        jboolean sync=false
) {
    CameraStreamHolder holder(
            env, source, dests, bitmapCallback, finishCallback,
            sourceProperties, destsProperties, fileDescriptors,
            sync
    );
    if(!holder.init()) {
        return false;
    }
    return holder.process_packets(last_pts);
}

void list_input_formats() {
    LOGI("list input formats.\n");
    AVInputFormat* input_format = av_iformat_next(NULL);
    while (input_format != NULL) {
        LOGI("find input format %s.\n", input_format->name);
        input_format = av_iformat_next(input_format);
    }
}

void list_output_formats() {
    LOGI("list output formats.\n");
    AVOutputFormat* output_format = av_oformat_next(NULL);
    while (output_format != NULL) {
        LOGI("find output format %s.\n", output_format->name);
        output_format = av_oformat_next(output_format);
    }
}

void list_codecs() {
    LOGI("list codecs.\n");
    void* i = 0;
    AVCodec* codec = NULL;
    while ((codec = (AVCodec*)av_codec_iterate(&i)) != NULL) {
        if (codec->type == AVMEDIA_TYPE_VIDEO) {
            LOGI("find codec %p = %s.\n", codec, codec->name);
            if (av_codec_is_encoder(codec)) {
                LOGI("codec %p %s is encoder.\n", codec, codec->name);
            }
            if (av_codec_is_decoder(codec)) {
                LOGI("codec %p %s is decoder.\n", codec, codec->name);
            }
        }
        // codec = av_codec_next(codec);
    }
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
    list_input_formats();
    list_output_formats();
    list_codecs();
}
