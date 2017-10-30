#include <jni.h>

#ifndef _Included_com_cwvs_jdd_utils_network_NativeInterface
#define _Included_com_cwvs_jdd_utils_network_NativeInterface
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_cwvs_jdd_utils_network_NativeInterface
 * Method:    startJNICTraceRoute
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_cwvs_jdd_utils_network_NativeInterface_startJNICTraceRoute
        (JNIEnv *, jobject, jstring);

#ifdef __cplusplus
extern "C" {
#endif
#endif


#undef com_cwvs_jdd_utils_network_NativeInterface_PORT
#define com_cwvs_jdd_utils_network_NativeInterface_PORT 80L
#undef com_cwvs_jdd_utils_network_NativeInterface_CONN_TIMES
#define com_cwvs_jdd_utils_network_NativeInterface_CONN_TIMES 4L

JNIEXPORT void JNICALL Java_com_cwvs_jdd_utils_network_NativeInterface_startJNITelnet
        (JNIEnv *, jobject, jstring, jstring);
