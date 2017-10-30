package com.cwvs.jdd.utils.network;

/**
 * @author liuchao on 2017/10/30.
 */

public class NetSocket {

    public NetSocket() {
    }

    public void start(String host, NetSocketListener listener) {
        NativeInterface nativeInterface = NativeInterface.getInstance();
        nativeInterface.setNetSocketListenr(listener);
        nativeInterface.startJNITelnet(host, "80");
    }

    public interface NetSocketListener {
        void onNetSocketFinished(String log);

        void onNetSocketUpdated(String log);
    }
}
