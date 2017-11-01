package com.cwvs.jdd.netcheck;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cwvs.jdd.network.analyse.NetAnalyzer;
import com.cwvs.jdd.network.analyse.NetUtils;

import java.net.InetAddress;

import jdd.cwvs.com.netcheck.R;

/**
 * @author liuchao
 */
public class NetCheckActivity extends AppCompatActivity {

    private TextView mMsgTv;
    private EditText mDomainUrlEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_net_check);
        findViewById(R.id.btnCheckNetwork).setOnClickListener(mOnclickListener);
        mMsgTv = (TextView) findViewById(R.id.tvMsg);
        mDomainUrlEt = (EditText) findViewById(R.id.etDomainUrl);
    }

    private void startCheck() {

        NetAnalyzer.Listener analyzerListener = new NetAnalyzer.Listener() {

            StringBuilder routeTraceStringBuilder = new StringBuilder();

            @Override
            public void onAnalyseStart() {
                mMsgTv.append("网络诊断开始...\n\n");
            }

            @Override
            public void onAnalyseFinish() {
                mMsgTv.append("网络诊断结束...\n\n");
            }

            @Override
            public void onModuleAnalyseStart(NetAnalyzer.Module module) {
                mMsgTv.append("\n" + module.getName() + "\n");
            }

            @Override
            public void onModuleAnalyseUpdate(NetAnalyzer.Module module, Object value) {
                switch (module) {
                    case TRACE_ROUTE:
                        String log = String.valueOf(value);
                        if (TextUtils.isEmpty(log)) {
                            return;
                        }
                        log = log.trim();
                        if (log.contains("ms") || log.contains("***")) {
                            routeTraceStringBuilder.append(log);
                            mMsgTv.append(routeTraceStringBuilder.toString() + "\n");
                            routeTraceStringBuilder.delete(0, routeTraceStringBuilder.length());
                        } else {
                            routeTraceStringBuilder.append(log);
                        }
                        break;
                    default:
                        mMsgTv.append(String.valueOf(value) + "\n");
                        break;
                }

            }

            @Override
            public void onModuleAnalyseFinish(NetAnalyzer.Module module, Object result) {
                switch (module) {
                    case ISP:
                    case NET_TYPE:
                    case PUBLIC_NETWORK_IP:
                    case LOCAL_DNS_INFO:
                        mMsgTv.append(String.valueOf(result) + "\n");
                        break;
                    case NET_SIGNAL_STRENGTH:
                        mMsgTv.append(String.valueOf(result) + "dbm\n");
                        break;
                    case DOMAIN_DNS_INFO:
                        NetUtils.DomainIpParseResult domainIp = (NetUtils.DomainIpParseResult) result;
                        InetAddress[] remoteInetAddress = domainIp.getRemoteInetAddress();
                        if (remoteInetAddress == null) {
                            mMsgTv.append("解析失败: 用时 " + domainIp.getUserTimeMillis() + "ms\n");
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (InetAddress address : remoteInetAddress) {
                                sb.append(address.getHostAddress()).append(',');
                            }
                            String ipString = sb.deleteCharAt(sb.length() - 1).toString();
                            mMsgTv.append("解析成功: " + ipString + "(" + domainIp.getUserTimeMillis() + "ms)\n");
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        new NetAnalyzer().analyse(this, mDomainUrlEt.getText().toString(), analyzerListener,
                NetAnalyzer.Module.ISP,
                NetAnalyzer.Module.NET_TYPE,
                NetAnalyzer.Module.NET_SIGNAL_STRENGTH,
                NetAnalyzer.Module.PUBLIC_NETWORK_IP,
                NetAnalyzer.Module.LOCAL_DNS_INFO,
                NetAnalyzer.Module.DOMAIN_DNS_INFO,
                NetAnalyzer.Module.PING,
                NetAnalyzer.Module.TRACE_ROUTE);
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
