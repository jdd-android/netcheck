package com.cwvs.jdd.network.analyse;

/**
 * @author liuchao on 2017/11/2.
 *         <p>
 *         网络诊断结果上报类。将网络诊断的结果上报服务器；
 */

public class Reporter {

    public void report() {
    }


    public static class ReportResult {

        // {\"wapName\":\"4g\",\"wapPower\":\"12\",\"ipAddress\":\"10.22.23.65\",\"serverIp\":\"10.22.23.65\",\"pingDelay\":\"4\"}

        private String wapName;
        private String wapPower;
        private String ipAddress;
        private String severIp;
        private String pingDelay;



    }
}
