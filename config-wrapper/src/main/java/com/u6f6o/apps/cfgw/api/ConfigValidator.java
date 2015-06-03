package com.u6f6o.apps.cfgw.api;

import com.google.common.collect.ImmutableMap;


public interface ConfigValidator {
    boolean onInit(ImmutableMap<String, String> config);
    boolean onUpdate(ImmutableMap<String, String> oldConfig, ImmutableMap<String, String> newConfig);
}
