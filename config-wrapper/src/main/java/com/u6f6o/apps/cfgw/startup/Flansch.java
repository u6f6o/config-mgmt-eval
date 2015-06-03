package com.u6f6o.apps.cfgw.startup;

import com.u6f6o.apps.cfgw.AutoRefreshingConfigMap;
import com.u6f6o.apps.cfgw.provider.consul.ConsulConfigFetcher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Flansch {
    private static final Logger LOGGER = Logger.getLogger(Flansch.class);

    public static void main(String[] args) throws InterruptedException {
        String consulClientIP = System.getenv("CONSUL_CLIENT_IP");
        consulClientIP = consulClientIP != null ? consulClientIP : System.getProperty("CONSUL_CLIENT_IP");
        Map<String, String> props = AutoRefreshingConfigMap.newConsulCatalogue(new ConsulConfigFetcher(consulClientIP));

        while(true) {

        }
    }
}
