package com.u6f6o.apps.cfgw.startup;

import com.u6f6o.apps.cfgw.AutoRefreshingConfigMap;
import com.u6f6o.apps.cfgw.ReadOnlyMap;
import com.u6f6o.apps.cfgw.provider.consul.ConsulConfigFetcher;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Flansch {

    public static void main(String[] args) throws InterruptedException {
        Map<String, String> props = AutoRefreshingConfigMap.newConsulCatalogue(new ConsulConfigFetcher());

        while(true) {
            for (Map.Entry<String, String> entry : props.entrySet()) {
                System.err.println(entry.getKey() + ": " + entry.getValue());
            }
            TimeUnit.SECONDS.sleep(20);
        }
    }
}
