LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE        := libvsim_jni

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../../../modules/libatci

LOCAL_SHARED_LIBRARIES := libcutils libutils liblog libhidlbase libhidltransport libhwbinder libandroid_runtime  vendor.sprd.hardware.radio@1.0
LOCAL_STATIC_LIBRARIES += libatci
LOCAL_LDFLAGS        := -llog

LOCAL_CPPFLAGS += $(JNI_CFLAGS)

LOCAL_CPP_EXTENSION := .cpp

LOCAL_SRC_FILES     := \
    src/vsimutils.cpp

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
