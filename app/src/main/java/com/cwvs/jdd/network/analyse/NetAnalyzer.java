package com.cwvs.jdd.network.analyse;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author liuchao on 2017/10/31.
 */

public class NetAnalyzer {

    private static final String TAG = NetAnalyzer.class.getSimpleName();

    /**
     * 开始诊断网络。
     *
     * @param context  {@link Context}
     * @param url      需要诊断的域名
     * @param listener 诊断结果回调
     * @param modules  需要诊断的模块，诊断的顺序与参数传递顺序一致。
     */
    public void analyse(Context context, String url, Listener listener, Module... modules) {
        ExecutorService executor = new ThreadPoolExecutor(
                1,
                1,
                3,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(12),
                new AnalyzerThreadFactory());

        listener.onAnalyseStart();
        if (modules == null || modules.length == 0) {
            listener.onAnalyseFinish();
            return;
        }

        Module lastModule = modules[modules.length - 1];

        for (Module module : modules) {
            BaseTask task;
            switch (module) {
                case ISP:
                    task = new IspTask();
                    break;
                case NET_TYPE:
                    task = new NetworkTypeTask();
                    break;
                case NET_SIGNAL_STRENGTH:
                    task = new NetSignalStrengthTask();
                    break;
                case PUBLIC_NETWORK_IP:
                    task = new PublicNetworkIpTask();
                    break;
                case LOCAL_DNS_INFO:
                    task = new LocalDnsInfoTask();
                    break;
                case DOMAIN_DNS_INFO:
                    task = new DomainDnsInfoTask();
                    break;
                case PING:
                    task = new PingTask();
                    break;
                case TRACE_ROUTE:
                    task = new TraceRouteTask();
                    break;
                default:
                    throw new IllegalArgumentException("未定义的网络诊断模块：" + module);
            }
            task.setContext(context)
                    .setUrl(url)
                    .setListener(new ListenerWrapper(listener).setAnalyseLastModule(lastModule));
            executor.submit(task);
        }
    }

    /**
     * 支持分析的模块。
     */
    public enum Module {
        /**
         * 运营商信息
         */
        ISP("运营商"),
        /**
         * 当前连接的网络类型
         */
        NET_TYPE("网络类型"),
        /**
         * 当前连接的网络信号强度
         */
        NET_SIGNAL_STRENGTH("信号强度"),
        /**
         * 公网 IP 地址
         */
        PUBLIC_NETWORK_IP("公网 IP"),
        /**
         * 本地 DNS 信息
         */
        LOCAL_DNS_INFO("本地 DNS"),
        /**
         * 域名解析信息
         */
        DOMAIN_DNS_INFO("服务节点 DNS"),
        /**
         * Ping 节点信息
         */
        PING("Ping"),
        /**
         * 追踪路由
         */
        TRACE_ROUTE("追踪路由");

        private String name;

        Module(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 网络分析过程以及结果的监听器。
     */
    public interface Listener {

        /**
         * 当分析开始时被回调
         */
        void onAnalyseStart();

        /**
         * 当分析完成时被回调
         */
        void onAnalyseFinish();

        /**
         * 当分析模块开始时被回调
         *
         * @param module 模块
         */
        void onModuleAnalyseStart(Module module);

        /**
         * 当分析模块过程更新时被回调
         *
         * @param module 模块
         * @param value  更新的信息
         */
        void onModuleAnalyseUpdate(Module module, Object value);

        /**
         * 当分析模块结束时被回调
         *
         * @param module 模块
         * @param result 分析结果
         */
        void onModuleAnalyseFinish(Module module, Object result);

    }


    /**
     * 获取运营商 TASK
     */
    private class IspTask extends BaseTask {

        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.ISP);
            String mobileOperator = NetUtils.getMobileOperator(getContext());
            getListener().onModuleAnalyseFinish(Module.ISP, mobileOperator);
        }
    }

    /**
     * 获取网络类型 TASK (2G,3G,4G,WIFI...)
     */
    private class NetworkTypeTask extends BaseTask {
        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.NET_TYPE);
            String netWorkType = NetUtils.getNetWorkType(getContext());
            getListener().onModuleAnalyseFinish(Module.NET_TYPE, netWorkType);
        }
    }

    /**
     * 获取当前连接网络信号强度 TASK
     */
    private class NetSignalStrengthTask extends BaseTask {
        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.NET_SIGNAL_STRENGTH);

            String netWorkType = NetUtils.getNetWorkType(getContext());
            // 如果当前连接的是 WIFI，直接获取信号强度并回调返回
            if (netWorkType.equals(NetUtils.NETWORK_TYPE_WIFI)) {
                int dbm = NetUtils.getWifiNetworkStrength(getContext());
                getListener().onModuleAnalyseFinish(Module.NET_SIGNAL_STRENGTH, dbm);
                return;
            }

            // 如果当前连接的是手机网络，监听 phoneState，获取信号强度后返回

            // 获取手机数据网络信号强度 API 是一个异步过程，因此需要锁定当前线程等待信号强度值被 API 回调
            // 获取 WiFi 信号强度 API 是一个同步过程，无需锁定线程
            // needLockCurrentThread 为了判断是否需要锁定线程等待
            final AtomicBoolean needLockCurrentThread = new AtomicBoolean(true);
            final Thread currentThread = Thread.currentThread();

            // 此处使用 HandlerThread 执行监听 phone state 的原因是，phone state 的监听逻辑必须在一个拥有 Looper 的线程中
            // 且 Looper 处在 loop 状态，否则无法获取到回调，由于获取信号强度的过程需要阻塞线程实现同步返回，所以无法在主线程中
            // 执行此处代码，在 HandlerThread 中执行比较合适
            HandlerThread handlerThread = new HandlerThread("HandlerThread");
            handlerThread.start();
            new Handler(handlerThread.getLooper()).post(new Runnable() {
                @Override
                public void run() {
                    NetUtils.getMobileNetworkStregth(getContext(), new NetUtils.NetworkStrengthListener() {
                        @Override
                        public void onGetStrength(int dbm) {
                            // 如果需要锁定线程，释放线程锁，让线程继续执行
                            if (needLockCurrentThread.getAndSet(false)) {
                                LockSupport.unpark(currentThread);
                            }
                            getListener().onModuleAnalyseFinish(Module.NET_SIGNAL_STRENGTH, dbm);
                        }
                    });
                }
            });

            // 如果需要锁定当前线程，则锁定 10 秒，等待
            if (needLockCurrentThread.get()) {
                // 锁定当前线程 10 秒，等待信号强度检测结果被回调后释放锁
                LockSupport.parkNanos(10 * 1_000_000_000L);
            }

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                handlerThread.quitSafely();
//            } else {
//                handlerThread.quit();
//            }
        }
    }

    /**
     * 获取外网 IP TASK
     */
    private class PublicNetworkIpTask extends BaseTask {
        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.PUBLIC_NETWORK_IP);
            String result;

            OkHttpClient client = new OkHttpClient.Builder()
                    .build();

            Request request = new Request.Builder()
                    .url("http://ip.taobao.com/service/getIpInfo2.php?ip=myip")
                    // 设置 User-Agent 为 Mozilla/5.0，否则接口无法返回 IP 地址信息
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .get()
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String bodyString = response.body().string();
                    JSONObject bodyJson = new JSONObject(bodyString);
                    int code = bodyJson.optInt("code", -1);
                    if (code == 0) {
                        result = bodyJson.getJSONObject("data").optString("ip", "NULL");
                    } else {
                        result = "ERR " + code;
                    }
                } else {
                    result = response.code() + response.message();
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = e.getMessage();
            }

            getListener().onModuleAnalyseFinish(Module.PUBLIC_NETWORK_IP, result);
        }
    }

    /**
     * 本地 DNS 解析 TASK
     */
    private class LocalDnsInfoTask extends BaseTask {
        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.LOCAL_DNS_INFO);
            String dns1 = NetUtils.getLocalDns("dns1");
            String dns2 = NetUtils.getLocalDns("dns2");
            getListener().onModuleAnalyseFinish(Module.LOCAL_DNS_INFO, dns1 + "," + dns2);
        }
    }

    /**
     * 域名解析 TASK
     */
    private class DomainDnsInfoTask extends BaseTask {
        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.DOMAIN_DNS_INFO);
            NetUtils.DomainIpParseResult domainIpParseResult = NetUtils.parseDomainIp(getUrl());
            getListener().onModuleAnalyseFinish(Module.DOMAIN_DNS_INFO, domainIpParseResult);
        }
    }

    /**
     * Ping TASK
     */
    private class PingTask extends BaseTask {
        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.PING);
            NetPing.execPing(getUrl(), 4, false, new NetPing.PingListener() {
                @Override
                public void onProgress(String progress) {
                    getListener().onModuleAnalyseUpdate(Module.PING, progress);
                }

                @Override
                public void onFinish(String result) {
                    // Ping result 解析规则

                    // 末尾带 DUP! 的数据需要丢弃，这是在 linux 平台会出现的一个数据包返回多个数据包的现象
                    // 64 bytes from 153.101.56.251: icmp_seq=4 ttl=56 time=13 ms (DUP!)

                    //  64 bytes from 181.138.212.118.adsl-pool.jx.chinaunicom.com (118.212.138.181): icmp_seq=1 ttl=52 time=39.3 ms

                    getListener().onModuleAnalyseFinish(Module.PING, result);
                }
            });
        }
    }

    /**
     * 路由追踪 TASK
     */
    private class TraceRouteTask extends BaseTask {
        @Override
        public void run() {
            getListener().onModuleAnalyseStart(Module.TRACE_ROUTE);
            new NetRouteTracer().startTrace(getUrl(), new NetRouteTracer.TraceListener() {
                @Override
                public void onTraceUpdate(String log) {
                    getListener().onModuleAnalyseUpdate(Module.TRACE_ROUTE, log);
                }

                @Override
                public void onTraceFinish() {
                    getListener().onModuleAnalyseFinish(Module.TRACE_ROUTE, "");
                }
            });
        }
    }

    private abstract class BaseTask implements Runnable {

        private Listener mListener;
        private Context mContext;
        private String mUrl;

        BaseTask setListener(Listener mListener) {
            this.mListener = mListener;
            return this;
        }

        Listener getListener() {
            return mListener;
        }

        BaseTask setContext(Context context) {
            this.mContext = context.getApplicationContext();
            return this;
        }

        Context getContext() {
            return mContext;
        }

        BaseTask setUrl(String url) {
            this.mUrl = url;
            return this;
        }

        String getUrl() {
            return mUrl;
        }
    }

    private class AnalyzerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "NetAnalyzerWorker");
        }
    }

    private class ListenerWrapper implements Listener {

        private Module mAnalyseLastModule;

        private Handler mHandler;

        private Listener mListener;

        private ListenerWrapper(Listener listener) {
            mListener = listener;
            mHandler = new Handler(Looper.getMainLooper());
        }

        ListenerWrapper setAnalyseLastModule(Module analyseLastModule) {
            this.mAnalyseLastModule = analyseLastModule;
            return this;
        }

        @Override
        public void onAnalyseStart() {
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onAnalyseStart();
                    }
                });
            }
        }

        @Override
        public void onAnalyseFinish() {
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onAnalyseFinish();
                    }
                });
            }
        }

        @Override
        public void onModuleAnalyseStart(final Module module) {
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onModuleAnalyseStart(module);
                    }
                });
            }
        }

        @Override
        public void onModuleAnalyseUpdate(final Module module, final Object value) {
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onModuleAnalyseUpdate(module, value);
                    }
                });
            }
        }

        @Override
        public void onModuleAnalyseFinish(final Module module, final Object result) {
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onModuleAnalyseFinish(module, result);
                        if (module == mAnalyseLastModule) {
                            mListener.onAnalyseFinish();
                        }
                    }
                });
            }
        }
    }

}
