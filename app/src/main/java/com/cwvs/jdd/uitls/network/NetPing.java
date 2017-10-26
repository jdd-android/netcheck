package com.cwvs.jdd.uitls.network;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liuchao on 2017/10/26.
 */

public class NetPing {

    private static final String MATCH_PING_IP = "(?<=from ).*(?=: icmp_seq=1 ttl=)";
    private static final String MATCH_PING_HOST_IP = "(?<=\\().*?(?=\\))";

    public static String execPing(String host, int sendCount, boolean isNeedL) {
        String execHost = host;

        Pattern p = Pattern.compile(MATCH_PING_HOST_IP);
        Matcher m = p.matcher(host);
        if (m.find()) {
            execHost = m.group();
        }

        String pingCmd = isNeedL ? "ping -s 8185 -c " : "ping -c ";
        Process process = null;
        BufferedReader reader = null;
        StringBuilder pingStatusBuilder = new StringBuilder();
        try {
            process = Runtime.getRuntime().exec(pingCmd + sendCount + " " + execHost);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                pingStatusBuilder.append(line).append("\n");
                Log.e("Ping", "Line: " + line);
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                // no-op
            }
        }

        String pingStatus = pingStatusBuilder.toString();
        Log.e("Ping", pingStatus);
        boolean pingOk = Pattern.compile(MATCH_PING_IP).matcher(pingStatus).find();
        if (pingOk) {
            return pingStatus;
        }
        if (TextUtils.isEmpty(pingStatus)) {
            return "unknown host or network error";
        }

        return pingStatusBuilder.toString();
    }

    public interface PingListener {
        void onStart(String host);

        void onProgress();
    }

    public static class PingStatus {

        private boolean isProgress;

        private String status;
        private String host;

        private int bytes;
        private int icmpSeq;
        private int ttl;
        private float time;

        private void parse(String status) {
            // 64 bytes from 180.97.33.107: icmp_seq=1 ttl=54 time=11.2 ms
            //
            if (TextUtils.isEmpty(status) || !status.contains("icmp_seq")) {
                isProgress = false;
                return;
            }

            // 64 bytes from 180.97.33.107: icmp_seq=1 ttl=54 time=11.2 ms
            //
            String[] split = status.split(" ");
            if (split.length >= 4) {

            }

        }
    }

}
