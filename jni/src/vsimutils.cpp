#define LOG_TAG "VSIM_JNI"

#include "utils/Log.h"
#include <string.h>
#include <jni.h>
#include <unistd.h>
#include "android_runtime/AndroidRuntime.h"

extern "C" {
#include "atci.h"
#include "utils.h"
#include "vsim.h"
}

namespace android {

#define UNUSED(x) (void*)x
#define MAX_COMMAND_BYTES               (8 * 1024)

static JavaVM* jvm = NULL;
static jmethodID callbackMethodId1;
static jmethodID callbackMethodId2;
static jobject obj1;
static jobject obj2;
static bool flag = false;

int vsim_command(unsigned char slot, unsigned char* apdu_req, unsigned short apdu_req_len, unsigned char* apdu_rsp, unsigned short apdu_rsp_len) {
    UNUSED(&apdu_rsp_len);
    ALOGD("vsim_command: slot = %d, apdu_req_len = %d", (int)slot, apdu_req_len);

    JNIEnv* env = NULL;
    JavaVMAttachArgs args;
    char name[] = "VSIM Service Callback Thread";
    args.version = JNI_VERSION_1_6;
    args.name = name;
    args.group = NULL;
    jvm->AttachCurrentThread(&env, &args);

    if(env == NULL) {
        ALOGD("vsim_command: env is null!");
    }


    if ((callbackMethodId1 == NULL && slot == 0) ||
                (callbackMethodId2 == NULL && slot == 1)) {
        int error_len = 2;
        apdu_rsp[0] = 0x98;
        apdu_rsp[1] = 0x64;
        return error_len;
    }
    int rsp_len = -1;
    jbyteArray bytes_req = env->NewByteArray(apdu_req_len);
    env->SetByteArrayRegion(bytes_req, 0, apdu_req_len, (jbyte*)apdu_req);
    jbyteArray bytes_rsp = NULL;
    if (slot == 0) {
        bytes_rsp = (jbyteArray) env->CallObjectMethod(obj1, callbackMethodId1, slot, bytes_req, apdu_req_len);
    } else {
        bytes_rsp = (jbyteArray) env->CallObjectMethod(obj2, callbackMethodId2, slot, bytes_req, apdu_req_len);
    }

    if (bytes_rsp == NULL) {
        int error_len = 2;
        apdu_rsp[0] = 0x98;
        apdu_rsp[1] = 0x64;
        return error_len;
    }
    jbyte* bytes = NULL;
    bytes = env->GetByteArrayElements(bytes_rsp, 0);
    rsp_len = env->GetArrayLength(bytes_rsp);

    for (int i = 0; i < rsp_len; i++) {
        apdu_rsp[i] = (unsigned char)bytes[i];
//        ALOGD("vsim_command: apdu_rsp[%d] = %d", i, apdu_rsp[i]);
    }
    env->ReleaseByteArrayElements(bytes_rsp, bytes, 0);
    env->DeleteLocalRef(bytes_rsp);
    env->DeleteLocalRef(bytes_req);

    //env->DeleteGlobalRef(obj);
    //obj = NULL;

    return rsp_len;
}

static JNIEXPORT jint vsim_Init_JNI(JNIEnv* env, jobject thiz, jint phoneId, jint restart) {
    ALOGD("init_Vsim_JNI: phoneId = %d, restart = %d", phoneId, restart);
    if (phoneId == 0) {
        if (obj1 == NULL) {
            obj1 = env->NewGlobalRef(thiz);
        }
        jclass localClass = env->GetObjectClass(thiz);
        callbackMethodId1 = env->GetMethodID(localClass, "serviceCallback", "(I[BI)[B");
        env->DeleteLocalRef(localClass);
    } else if (phoneId == 1){
        if (obj2 == NULL) {
            obj2 = env->NewGlobalRef(thiz);
        }
        jclass localClass = env->GetObjectClass(thiz);
        callbackMethodId2 = env->GetMethodID(localClass, "serviceCallback", "(I[BI)[B");
        env->DeleteLocalRef(localClass);
    }


    return (jint)vsim_init(phoneId, vsim_command, restart);
}

JNIEXPORT jint vsim_Send_Data_JNI(JNIEnv* env, jobject thiz, jint phoneId, jbyteArray data, jint length) {
    UNUSED(env);
    UNUSED(thiz);
    ALOGD("vsim_Send_Data_JNI: phoneId = %d, length = %d", phoneId, length);
    jbyte* bytes = NULL;
    jint res = 0;

    bytes = env->GetByteArrayElements(data, 0);

    res = (jint)vsim_send_data(phoneId, (unsigned char*)bytes, length);
    ALOGD("vsim_Send_Data_JNI: res = %d", res);
    env->ReleaseByteArrayElements(data, bytes, 0);

    return res;
}

JNIEXPORT jint vsim_Exit_JNI(JNIEnv* env, jobject thiz, jint phoneId) {
    UNUSED(thiz);
    ALOGD("vsim_Exit_JNI phoneId = %d", phoneId);
    jint result = (jint)vsim_exit(phoneId);
    if (phoneId == 0) {
        env->DeleteGlobalRef(obj1);
        obj1 = NULL;
        callbackMethodId1 = NULL;
    } else if (phoneId == 1){
        env->DeleteGlobalRef(obj2);
        obj2 = NULL;
        callbackMethodId2 = NULL;
    }
    return result;
}

JNIEXPORT jint vsim_Set_Authid_JNI(JNIEnv* env, jobject thiz, jint authid) {
    UNUSED(env);
    UNUSED(thiz);
    ALOGD("vsim_Set_Authid_JNI: authId = %d", authid);
    return (jint)vsim_set_authid(authid);
}

JNIEXPORT jint vsim_Query_Authid_JNI(JNIEnv* env, jobject thiz) {
    UNUSED(env);
    UNUSED(thiz);
    ALOGD("vsim_Query_Authid_JNI");
    return (jint)vsim_query_authid();
}

JNIEXPORT jint vsim_Set_Virtual_JNI(JNIEnv* env, jobject thiz, jint phoneId, jint mode) {
    UNUSED(env);
    UNUSED(thiz);
    ALOGD("vsim_Set_Virtual_JNI: phoneId = %d, modem = %d", phoneId, mode);
    return (jint)vsim_set_virtual(phoneId, mode);
}

JNIEXPORT jint vsim_Set_Virtual_WithNV_JNI(JNIEnv* env, jobject thiz, jint phoneId, jint mode, jint writeNv) {
    UNUSED(env);
    UNUSED(thiz);
    ALOGD("vsim_Set_Virtual_WithNV_JNI: phoneId = %d, modem = %d, writeNv = %d", phoneId, mode, writeNv);
    return (jint)vsim_set_nv(phoneId, mode, writeNv);
}

JNIEXPORT jint vsim_Query_Virtual_JNI(JNIEnv* env, jobject thiz, jint phoneId) {
    UNUSED(env);
    UNUSED(thiz);
    ALOGD("vsim_Query_Virtual_JNI: phoneId = %d", phoneId);
    return (jint)vsim_query_virtual(phoneId);
}

JNIEXPORT jint vsim_Get_Auth_Cause_JNI(JNIEnv* env, jobject thiz, jint phoneId) {
    UNUSED(env);
    UNUSED(thiz);
    ALOGD("vsim_Get_Auth_Cause_JNI");
    return (jint)vsim_get_auth_cause(phoneId);
}

JNIEXPORT jbyteArray get_apdu_from_rsim_JNI(JNIEnv* env, jobject thiz, jint phoneId, jbyteArray apdu_req) {
    UNUSED(env);
    UNUSED(thiz);

    ALOGD("get_apdu_from_rsim_JNI:phoneId = %d", phoneId);
    jbyte* bytes = NULL;
    bytes = env->GetByteArrayElements(apdu_req, 0);
    int req_len = env->GetArrayLength(apdu_req);

    ALOGD("get_apdu_from_rsim_JNI: req_len = %d", req_len);
    jbyte* rsp_bytes = new jbyte[4096];
    int rsp_len = vsim_parse_apdu(phoneId, (unsigned char*)bytes, req_len, (unsigned char*)rsp_bytes, 2048);

    ALOGD("get_apdu_from_rsim_JNI: rsp_len = %d", rsp_len);
    jbyteArray apdu_rsp = env->NewByteArray(rsp_len);
    env->SetByteArrayRegion(apdu_rsp, 0, rsp_len, rsp_bytes);

    delete[] rsp_bytes;
    rsp_bytes = NULL;
    env->ReleaseByteArrayElements(apdu_req, bytes, 0);
    return apdu_rsp;
}

JNIEXPORT jstring send_AT_Cmd_JNI(JNIEnv* env, jobject thiz, jint phoneId, jstring cmd) {
   UNUSED(thiz);

   char result[MAX_COMMAND_BYTES] = {0};
   const char* atCmd = env->GetStringUTFChars(cmd, 0);
//   ALOGD("send_AT_Cmd_JNI: ATcmd = %s", atCmd);
   int resultValue = sendATCmd(phoneId, atCmd, result, MAX_COMMAND_BYTES);
   env->ReleaseStringUTFChars(cmd, atCmd);

   if (resultValue != 0) {
        return env->NewStringUTF("ERROR");
   }

//   ALOGI("the return value is: %s", result);
   return env->NewStringUTF(result);
}

static const char *objectClassPathName =
        "com/sprd/vsimservice/VSIMInterfaceService";

static JNINativeMethod getMethods[] = {
        {"vsimInitNative","(II)I", (void*)vsim_Init_JNI},
        {"vsimSendDataNative", "(I[BI)I", (void*)vsim_Send_Data_JNI},
        {"vsimExitNative", "(I)I", (void*)vsim_Exit_JNI},
        {"vsimSetAuthidNative", "(I)I", (void*)vsim_Set_Authid_JNI},
        {"vsimQueryAuthidNative", "()I", (void*)vsim_Query_Authid_JNI},
        {"vsimSetVirtualNative", "(II)I", (void*)vsim_Set_Virtual_JNI},
        {"vsimSetVirtualWithNVNative", "(III)I", (void*)vsim_Set_Virtual_WithNV_JNI},
        {"vsimQueryVirtualNative", "(I)I", (void*)vsim_Query_Virtual_JNI},
        {"vsimGetAuthCauseNative", "(I)I", (void*)vsim_Get_Auth_Cause_JNI},
        {"getAPDUFromRsimNative", "(I[B)[B", (void*) get_apdu_from_rsim_JNI},
        {"sendATCmdNative", "(ILjava/lang/String;)Ljava/lang/String;", (void*) send_AT_Cmd_JNI},
};

static int registerNativeMethods(JNIEnv* env, const char* className,
        JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("RegisterNatives failed for '%s'", className);
        env->DeleteLocalRef(clazz);
        return JNI_FALSE;
    }
    env->DeleteLocalRef(clazz);
    return JNI_TRUE;
}

}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    UNUSED(reserved);
    JNIEnv* env;

    android::jvm = vm;
    //use JNI1.6
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("Error: GetEnv failed in JNI_OnLoad");
        return -1;
    }
    if (!android::registerNativeMethods(env, android::objectClassPathName, android::getMethods,
            sizeof(android::getMethods) / sizeof(android::getMethods[0]))) {
        ALOGE("Error: could not register native methods for VSIM DEMO");
        return -1;
    }
      return JNI_VERSION_1_6;
}
