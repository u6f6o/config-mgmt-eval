package com.u6f6o.apps.cfgw.api;

import java.util.Map;


public interface ConfigFetcher {

    Map<String, String> fetchLatestConfig();
}
