package com.cwvs.jdd.network.analyse;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Locale;

/**
 * @author liuchao on 2017/10/25.
 */

public class NetUtils {

    /**
     * 2G 网络类型
     */
    public static final String NETWORK_TYPE_2G = "2G";
    /**
     * 3G 网络类型
     */
    public static final String NETWORK_TYPE_3G = "3G";
    /**
     * 4G 网络类型
     */
    public static final String NETWORK_TYPE_4G = "4G";
    /**
     * WAP 网络类型
     */
    public static final String NETWORK_TYPE_WAP = "WAP";
    /**
     * WIFI 网络类型
     */
    public static final String NETWORK_TYPE_WIFI = "WIFI";
    /**
     * 无网络
     */
    public static final String NETWORK_TYPE_INVALID = "NO-NETWORK";
    /**
     * 未知网络类型
     */
    public static final String NETWORK_TYPE_UNKNOWN = "UNKNOWN";

    /**
     * 运营商类型 中国移动
     */
    public static final String MOBILE_OPERATOR_CMCC = "中国移动";
    /**
     * 运营商类型 中国联通
     */
    public static final String MOBILE_OPERATOR_WCDMA = "中国联通";
    /**
     * 运营商类型 中国电信
     */
    public static final String MOBILE_OPERATOR_CDMA = "中国电信";
    /**
     * 运营商类型 未知类型
     */
    public static final String MOBILE_OPERATOR_UNKNOWN = "未知运营商";

    /**
     * 判断是否连接网络。
     *
     * @param context {@link Context}
     * @return 如果连接到了网络，返回 true，反之，返回 false
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }

        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        return networkinfo != null && networkinfo.isAvailable();
    }

    /**
     * 获取网络类型。
     * 2G 3G 4G WIFI WAP UNKNOWN NO-NETWORK
     *
     * @param context {@link Context}
     * @return 2G 3G 4G WIFI WAP UNKNOWN NO-NETWORK
     */
    @SuppressWarnings("deprecation")
    public static String getNetWorkType(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return NETWORK_TYPE_UNKNOWN + "[NON-CM]";
        }

        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return NETWORK_TYPE_UNKNOWN + "[NON-NI]";
        }
        if (!networkInfo.isConnected()) {
            return NETWORK_TYPE_INVALID;
        }

        String networkType = networkInfo.getTypeName();
        String networkTypeNameWifi = "WIFI";
        String networkTypeNameMobile = "MOBILE";
        if (networkType.equalsIgnoreCase(networkTypeNameWifi)) {
            return NETWORK_TYPE_WIFI;
        }
        if (networkType.equalsIgnoreCase(networkTypeNameMobile)) {
            String proxyHost = android.net.Proxy.getDefaultHost();
            if (TextUtils.isEmpty(proxyHost)) {
                return mobileNetworkType(context);
            }
        }

        return NETWORK_TYPE_WAP;
    }

    /**
     * 获取运营商名称
     *
     * @param context {@link Context}
     * @return 返回运营商名称
     */
    public static String getMobileOperator(Context context) {
        TelephonyManager telManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (telManager == null) {
            return "未知运营商";
        }

        // 移动运营商代码
        String cmcc1 = "46000", cmcc2 = "46002", cmcc3 = "46007";
        // 联通运营商代码
        String wcdma = "46001";
        // 电信运营商代码
        String cdma = "46003";

        String operator = telManager.getSimOperator();
        if (TextUtils.isEmpty(operator)) {
            return MOBILE_OPERATOR_UNKNOWN;
        }
        if (operator.equals(cmcc1) || operator.equals(cmcc2)
                || operator.equals(cmcc3)) {
            return MOBILE_OPERATOR_CMCC;
        }
        if (operator.equals(wcdma)) {
            return MOBILE_OPERATOR_WCDMA;
        }
        if (operator.equals(cdma)) {
            return MOBILE_OPERATOR_CDMA;
        }

        return MOBILE_OPERATOR_UNKNOWN;
    }

    /**
     * 获取网络信号强度。
     *
     * @param context  {@link Context}
     * @param listener 获取信号强度回调
     */
    public static void getNetworkStrength(Context context, NetworkStrengthListener listener) {
        String netWorkType = getNetWorkType(context);
        if (netWorkType.equals(NETWORK_TYPE_WIFI)) {
            listener.onGetStrength(getWifiNetworkStrength(context));
        } else {
            getMobileNetworkStrength(context, listener);
        }
    }

    /**
     * 获取 WiFi 网络信号强度；
     *
     * @param context
     * @return
     */
    public static int getWifiNetworkStrength(Context context) {
        Context appContext = context.getApplicationContext();
        WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        return info.getRssi();
    }

    /**
     * 获取手机网络信号强度。
     *
     * @param context  {@link Context}
     * @param listener 信号强度回调
     */
    public static void getMobileNetworkStrength(Context context, final NetworkStrengthListener listener) {
        final Context appContext = context.getApplicationContext();
        final TelephonyManager telephonyManager = (TelephonyManager) appContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener phoneListener = new PhoneStateListener() {

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                int dbm;
                String mobileNetworkType = mobileNetworkType(appContext);
                if (mobileNetworkType.equalsIgnoreCase(NETWORK_TYPE_2G)
                        || mobileNetworkType.equalsIgnoreCase(NETWORK_TYPE_3G)) {
                    int asu = signalStrength.getGsmSignalStrength();
                    dbm = -113 + 2 * asu;
                } else {
                    String signalInfo = signalStrength.toString();
                    String[] parts = signalInfo.split(" ");
                    dbm = Integer.parseInt(parts[9]);
                    if (dbm > 0) {
                        // 第一次解析返回的值是 2147483647（Integer.MAX_VALUE），
                        // 此处判断如果大于 0，等待下一次返回正常值时再回调
                        return;
                    }
                }
                listener.onGetStrength(dbm);
                // 注销当前的手机状态变化监听
                telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
            }
        };
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    /**
     * 手机网络强度监听器；
     * 由于获取手机网络强度时异步方法，因此创建此监听器获取信号强度。
     *
     * @see #getMobileNetworkStrength(Context, NetworkStrengthListener)
     */
    public interface NetworkStrengthListener {
        /**
         * 当获取到了手机网络信号强度时被回调
         *
         * @param dbm 信号强度
         */
        void onGetStrength(int dbm);
    }

    /**
     * 获取本机IP(wifi)
     *
     * @param context {@link Context}
     * @return ip
     */
    public static String getLocalIpByWifi(Context context) {
        Context appContext = context.getApplicationContext();
        WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return "NULL";
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return "NULL";
        }
        int ipAddress = wifiInfo.getIpAddress();
        return String.format(Locale.US, "%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

    /**
     * 获取本机IP(2G/3G/4G)
     */
    public static String getLocalIpBy3G() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                        .hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NULL";
    }

    /**
     * wifi状态下获取网关
     */
    public static String pingGateWayInWifi(Context context) {
        String gateWay = null;
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return "NULL";
        }
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo != null) {
            int tmp = dhcpInfo.gateway;
            gateWay = String.format(Locale.US, "%d.%d.%d.%d", (tmp & 0xff), (tmp >> 8 & 0xff),
                    (tmp >> 16 & 0xff), (tmp >> 24 & 0xff));
        }
        return gateWay;
    }

    /**
     * 获取本地DNS
     */
    public static String getLocalDns(String dns) {
        Process process = null;
        String str = "";
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec("getprop net." + dns);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                str += line;
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                process.destroy();
            } catch (Exception e) {
                // no-op
            }
        }
        return str.trim();
    }

    /**
     * 解析域名 IP 地址；
     *
     * @param domain 域名
     * @return 解析结果
     */
    public static DomainIpParseResult parseDomainIp(String domain) {
        DomainIpParseResult result = new DomainIpParseResult();
        long startTimeMillis = System.currentTimeMillis();
        long endTimeMillis;

        try {
            InetAddress[] inetAddressArray = InetAddress.getAllByName(domain);
            endTimeMillis = System.currentTimeMillis();
            result.userTimeMillis = endTimeMillis - startTimeMillis;
            result.remoteInetAddress = inetAddressArray;
        } catch (UnknownHostException e) {
            endTimeMillis = System.currentTimeMillis();
            result.userTimeMillis = endTimeMillis - startTimeMillis;
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 域名解析结果；
     */
    public static class DomainIpParseResult {

        private long userTimeMillis;
        @Nullable
        private InetAddress[] remoteInetAddress;

        public long getUserTimeMillis() {
            return userTimeMillis;
        }

        public InetAddress[] getRemoteInetAddress() {
            return remoteInetAddress;
        }
    }

    /**
     * 返回手机网络的类型 2G 3G 4G WIFI.
     *
     * @param context {@link Context}
     * @return 返回手机网络的类型 2G 3G 4G WIFI or UNKNOWN
     */
    private static String mobileNetworkType(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return NETWORK_TYPE_UNKNOWN + "[NON-TM]";
        }
        switch (telephonyManager.getNetworkType()) {
            // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return NETWORK_TYPE_2G;
            // ~ 14-64 kbps
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return NETWORK_TYPE_2G;
            // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return NETWORK_TYPE_2G;
            // ~ 400-1000 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return NETWORK_TYPE_3G;
            // ~ 600-1400 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return NETWORK_TYPE_3G;
            // ~ 100 kbps
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return NETWORK_TYPE_2G;
            // ~ 2-14 Mbps
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return NETWORK_TYPE_3G;
            // ~ 700-1700 kbps
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return NETWORK_TYPE_3G;
            // ~ 1-23 Mbps
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return NETWORK_TYPE_3G;
            // ~ 400-7000 kbps
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return NETWORK_TYPE_3G;
            // ~ 1-2 Mbps
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return NETWORK_TYPE_3G;
            // ~ 5 Mbps
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return NETWORK_TYPE_3G;
            // ~ 10-20 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORK_TYPE_3G;
            // ~25 kbps
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_TYPE_2G;
            // ~ 10+ Mbps
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NETWORK_TYPE_4G;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return NETWORK_TYPE_UNKNOWN;
            default:
                return NETWORK_TYPE_4G;
        }
    }
}
