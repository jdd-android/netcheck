package com.cwvs.jdd.netcheck;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cwvs.jdd.utils.network.NetPing;
import com.cwvs.jdd.utils.network.NetRouteTracer;
import com.cwvs.jdd.utils.network.NetUtils;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jdd.cwvs.com.netcheck.R;

/**
 * @author liuchao
 */
public class NetCheckActivity extends AppCompatActivity {

    private TextView mMsgTv;
    private EditText mDomainUrlEt;

    private NetCheckActivity self;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        self = this;
        setContentView(R.layout.activity_net_check);
        findViewById(R.id.btnCheckNetwork).setOnClickListener(mOnclickListener);
        mMsgTv = (TextView) findViewById(R.id.tvMsg);
        mDomainUrlEt = (EditText) findViewById(R.id.etDomainUrl);

        mHandler = new Handler(getMainLooper());
    }

    private void startCheck() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        String netWorkType = NetUtils.getNetWorkType(self);
        String mobileOperator = NetUtils.getMobileOperator(self);

        appenText("运营商：" + mobileOperator);
        appenText("网络类型：" + netWorkType);

        NetUtils.getNetworkStrength(self, new NetUtils.NetworkStrengthListener() {
            @Override
            public void onGetStrength(int dbm) {
                appenText("当前连接网络强度：" + dbm + " dBm");
            }
        });

        int wifiDbm = NetUtils.getWifiNetworkStrength(self);
        appenText("WiFi网络强度：" + wifiDbm + " dBm");

        NetUtils.getMobileNetworkStregth(self, new NetUtils.NetworkStrengthListener() {
            @Override
            public void onGetStrength(int dbm) {
                appenText("手机网络强度：" + dbm + " dBm");
            }
        });

        final String url = mDomainUrlEt.getText().toString();
        executor.submit(new Runnable() {
            @Override
            public void run() {

                // 获取本机 IP 地址
                if (NetUtils.getNetWorkType(getApplicationContext()).equals(NetUtils.NETWORK_TYPE_WIFI)) {
                    String localIpByWifi = NetUtils.getLocalIpByWifi(getApplicationContext());
                    appenText("Ip : " + localIpByWifi);
                    String gateWayInWifi = NetUtils.pingGateWayInWifi(getApplicationContext());
                    appenText("网关 : " + gateWayInWifi);
                } else {
                    String localIpBy3G = NetUtils.getLocalIpBy3G();
                    appenText("Ip : " + localIpBy3G);
                }

                // 本地 DNS 解析
                String dns1 = NetUtils.getLocalDns("dns1");
                String dns2 = NetUtils.getLocalDns("dns2");
                appenText("本地 DNS : " + dns1 + "   " + dns2);

                // 域名解析
                appenText("\n解析域名: " + url);
                NetUtils.DomainIpParseResult domainIp = NetUtils.parseDomainIp(url);
                InetAddress[] remoteInetAddress = domainIp.getRemoteInetAddress();
                if (remoteInetAddress == null) {
                    appenText("解析失败: 用时 " + domainIp.getUserTimeMillis() + "ms");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (InetAddress address : remoteInetAddress) {
                        sb.append(address.getHostAddress()).append(',');
                    }
                    String ipString = sb.deleteCharAt(sb.length() - 1).toString();
                    appenText("解析成功: " + ipString + "(" + domainIp.getUserTimeMillis() + "ms)");
                }

                if (remoteInetAddress != null && remoteInetAddress.length > 0) {
                    InetAddress host = remoteInetAddress[0];
                    appenText("\nPing " + host.getHostAddress());
                    String result = NetPing.execPing(host.getHostAddress(), 4, false);
                    appenText(result);
                } else {
                    appenText("\nparse domain ip err, can't exec ping task");
                }

                appenText("\n开始追踪路由...");
                appenText(new NetRouteTracer().stringFromJni());
                new NetRouteTracer().startTrace(url, new NetRouteTracer.TraceListener() {
                    @Override
                    public void onTraceUpdate(String log) {
                        appenText(log);
                    }

                    @Override
                    public void onTraceFinish() {

                    }
                });

            }
        });

    }

    private void appenText(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mMsgTv.append(text + "\n");
            }
        });
    }

    private View.OnClickListener mOnclickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            switch (id) {
                case R.id.btnCheckNetwork:
                    mMsgTv.setText("");
                    startCheck();
                    break;
                default:
                    break;
            }
        }
    };
}
