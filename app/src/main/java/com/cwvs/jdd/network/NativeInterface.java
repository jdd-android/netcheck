package com.cwvs.jdd.network;

import android.util.Log;

import com.cwvs.jdd.network.analyse.NetRouteTracer;
import com.cwvs.jdd.network.analyse.NetSocket;

/**
 * @author liuchao on 2017/10/30.
 */

public class NativeInterface {
    private static final String TAG = NativeInterface.class.getSimpleName();

    private static volatile NativeInterface instance = null;

    private static boolean mLibLoaded = false;

    private NetRouteTracer.TraceListener mTraceListener;
    private NetSocket.NetSocketListener mNetSocketListenr;

    private NativeInterface() {
    }

    public static NativeInterface getInstance() {
        if (instance == null) {
            synchronized (NativeInterface.class) {
                instance = new NativeInterface();
            }
        }
        return instance;
    }

    static {
        System.loadLibrary("traceroute");
        mLibLoaded = true;
    }

    public void setTraceListener(NetRouteTracer.TraceListener traceListener) {
        this.mTraceListener = traceListener;
    }

    public void setNetSocketListenr(NetSocket.NetSocketListener netSocketListenr) {
        this.mNetSocketListenr = netSocketListenr;
    }

    /**
     * JNI C 函数接口
     *
     * @param host 域名
     */
    public native void startJNICTraceRoute(String host);

    public native void startJNITelnet(String host, String port);

    /**
     * 由 JNI 模块调用，回调 trace 信息。不可随意更改本函数签名。
     *
     * @param log 信息
     */
    public void onTraceUpdate(String log) {
        Log.e(TAG, log);
        if (mTraceListener != null) {
            mTraceListener.onTraceUpdate(log);
        }
    }

    /**
     * 由 JNI 模块调用，当路由追踪结束时被回调。不可随意更改本函数签名
     *
     * @param log 信息
     */
    public void onTraceFinish(String log) {
        Log.e(TAG, log);
        if (mTraceListener != null) {
            mTraceListener.onTraceFinish();
        }
    }

    public void printSocketInfo(String log) {
        Log.e(TAG, log);
        if (mNetSocketListenr != null) {
            mNetSocketListenr.onNetSocketUpdated(log);
        }
    }
}
