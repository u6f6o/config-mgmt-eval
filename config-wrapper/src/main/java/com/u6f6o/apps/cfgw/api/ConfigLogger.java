package com.u6f6o.apps.cfgw.api;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public interface ConfigLogger {
    void onUpdate(ImmutableMap<String, String> oldConfig, Map<String, String> newConfig);
}
