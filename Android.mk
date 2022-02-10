LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += \ $(call all-Iaidl-files-under, src)

#LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src/

LOCAL_JAVA_LIBRARIES := radio_interactor_common

LOCAL_PACKAGE_NAME := VsimService

LOCAL_JNI_SHARED_LIBRARIES := libvsim_jni
#LOCAL_JNI_SHARED_LIBRARIES += libatci

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
