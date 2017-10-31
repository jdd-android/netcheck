#include <string.h>
#include <jni.h>
#include <android/log.h>

// 将路由追踪信息回调给 Java 层
void callbackTraceInfo2Java(const char *aStrToPrint);

// 当路由追踪结束时被调用，回调给 Java 层
void callbackTraceFinish2Java(const char *aStrToPrint);

// 当 Tcp 连接测试数据回调给 Java 层
void PrintSocketInfo(const char *aStrToPrint);
