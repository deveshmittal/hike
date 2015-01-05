LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := \
	libspeexdsp/resample.c

LOCAL_MODULE:= libspeexresampler
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS+= -DEXPORT= -DFIXED_POINT -O3 -fstrict-aliasing -fprefetch-loop-arrays

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/include

include $(BUILD_SHARED_LIBRARY)

