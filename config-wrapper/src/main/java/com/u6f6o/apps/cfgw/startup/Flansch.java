package com.u6f6o.apps.cfgw.startup;

import com.u6f6o.apps.cfgw.AutoRefreshingConfigMap;
import com.u6f6o.apps.cfgw.provider.consul.ConsulConfigFetcher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Flansch {

    public static void main(String[] args) throws InterruptedException {
        Map<String, String> props = AutoRefreshingConfigMap.newConsulCatalogue(new ConsulConfigFetcher());

        while(true) {
            try {
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    System.err.println(entry.getKey() + ": " + StringUtils.newStringUtf8(
                            Base64.decodeBase64(entry.getValue())));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            TimeUnit.SECONDS.sleep(20);
        }
    }
}
