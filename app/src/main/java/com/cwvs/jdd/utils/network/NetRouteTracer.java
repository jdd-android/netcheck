package com.cwvs.jdd.utils.network;

/**
 * @author liuchao on 2017/10/27.
 */

public class NetRouteTracer {

    private static final String TAG = NetRouteTracer.class.getSimpleName();

    private NativeInterface mNativeInterface;

    public NetRouteTracer() {
    }

    public void startTrace(String host, TraceListener listener) {
        NativeInterface nativeInterface = NativeInterface.getInstance();
        nativeInterface.setTraceListener(listener);
        nativeInterface.startJNICTraceRoute(host);
    }

    public interface TraceListener {
        void onTraceUpdate(String log);

        void onTraceFinish();
    }
}
