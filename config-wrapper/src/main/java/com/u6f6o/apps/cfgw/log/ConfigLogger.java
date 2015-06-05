package com.u6f6o.apps.cfgw.log;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public interface ConfigLogger {
    void logOnUpdate(ImmutableMap<String, String> cachedConfig, Map<String, String> upToDateConfig);
    void logOnInit(ImmutableMap<String, String> initialConfig);
}
