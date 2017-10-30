package com.cwvs.jdd.netcheck;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


import com.cwvs.jdd.utils.network.NetUtils;
import com.task.TraceTask;

import jdd.cwvs.com.netcheck.R;

/**
 * @author liuchao
 */
public class MainActivity extends AppCompatActivity {

    private TextView mMsgTv;
    private EditText mDomainUrlEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnCheckNetwork).setOnClickListener(mOnclickListener);
        findViewById(R.id.btnGotoTest).setOnClickListener(mOnclickListener);
        mMsgTv = (TextView) findViewById(R.id.tvMsg);
        mDomainUrlEt = (EditText) findViewById(R.id.etDomainUrl);

        String netWorkType = NetUtils.getNetWorkType(this);
        String mobileOperator = NetUtils.getMobileOperator(this);
        Log.e("NetWork", "网络类型：" + netWorkType);
        Log.e("NetWork", "运营商：" + mobileOperator);
        NetUtils.getNetworkStrength(this, new NetUtils.NetworkStrengthListener() {
            @Override
            public void onGetStrength(int dbm) {
                Log.e("NetWork", "网络强度：" + dbm);
            }
        });
        int wifiDbm = NetUtils.getWifiNetworkStrength(this);
        Log.e("NetWork", "wifi dbm：" + wifiDbm);
        NetUtils.getMobileNetworkStregth(this, new NetUtils.NetworkStrengthListener() {
            @Override
            public void onGetStrength(int dbm) {
                Log.e("NetWork", "手机网络强度：" + dbm);
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
                    TraceTask pingTask = new TraceTask(MainActivity.this, mDomainUrlEt.getText().toString(), mMsgTv);
                    pingTask.doTask();
                    break;
                case R.id.btnGotoTest:
                    startActivity(new Intent(MainActivity.this, NetCheckActivity.class));
                    break;
                default:
                    break;
            }
        }
    };
}
