package com.cwvs.jdd.utils.network;

import android.util.Log;

/**
 * @author liuchao on 2017/10/27.
 */

public class NetRouteTracer {

    private static final String TAG = NetRouteTracer.class.getSimpleName();

    /**
     * JNI 库是否被成功加载
     */
    private static boolean jniLoaded = false;

    private TraceListener traceListener;

    static {

            System.loadLibrary("traceroute");
            jniLoaded = true;

    }

    public void startTrace(String host, TraceListener listener) {
        this.traceListener = listener;
        startJNICTraceRoute(host);
    }

    /**
     * JNI C 函数接口
     *
     * @param host
     */
    public native void startJNICTraceRoute(String host);

    public native String stringFromJni();

    /**
     * 由 JNI 模块调用，回调 trace 信息
     *
     * @param log 信息
     */
    public void printTraceInfo(String log) {
        Log.e(TAG, log);
        if (traceListener != null) {
            traceListener.onTraceUpdate(log);
        }
    }


    public interface TraceListener {
        void onTraceUpdate(String log);

        void onTraceFinish();
    }
}
