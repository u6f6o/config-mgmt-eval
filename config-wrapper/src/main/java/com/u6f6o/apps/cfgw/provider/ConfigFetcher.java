package com.u6f6o.apps.cfgw.provider;

import java.util.Map;


public interface ConfigFetcher {

    Map<String, String> fetchLatestConfig();
}
