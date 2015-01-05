#include "speexjni.h"
#include <android/log.h>

#define  LOG_TAG    "NDK Message"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

inline jbyte* get_byte_array(JNIEnv* env, jbyteArray pcm) {
	jboolean isCopy;
	return (*env)->GetByteArrayElements(env, pcm, &isCopy);
}

inline void release_byte_array(JNIEnv* env, jbyteArray pcm, jbyte* data, jint mode) {
	(*env)->ReleaseByteArrayElements(env, pcm, data, mode);
}


JNIEXPORT jint JNICALL JNI_OnLoad( JavaVM *vm, void *pvt )
{
	LOGD("Speex loaded.");
	return JNI_VERSION_1_2;
}

JNIEXPORT jlong JNICALL Java_com_bsb_hike_voip_SpeexDSPWrapper_resampler_1create
(JNIEnv * je, jobject jo, jint inputRate, jint outputRate, jint quality) {

	jint errors = 0;
	jlong resampler = speex_resampler_init(1, inputRate, outputRate, quality, &errors);
	return resampler;

}

JNIEXPORT void JNICALL Java_com_bsb_hike_voip_SpeexDSPWrapper_resampler_1destroy
(JNIEnv * je, jobject jo, jlong resampler) {

	speex_resampler_destroy(resampler);
}

JNIEXPORT jint JNICALL Java_com_bsb_hike_voip_SpeexDSPWrapper_resampler_1resample
  (JNIEnv * je, jobject jo, jlong resampler, jbyteArray input, jint inputLength, jbyteArray output, jint outputLength) {

	  jint retVal;
	  jbyte *in, *out;

	  in = get_byte_array(je, input);
	  out = get_byte_array(je, output);
	  LOGD("Pre inputlength: %d, outputLength: %d", inputLength, outputLength);
	  retVal = speex_resampler_process_int(resampler, 0, in, &inputLength, out, &outputLength);
	  LOGD("Post inputlength: %d, outputLength: %d, retval: %d", inputLength, outputLength, retVal);
	  release_byte_array(je, input, in, JNI_ABORT);
	  release_byte_array(je, output, out, 0);

	  return retVal;
}
