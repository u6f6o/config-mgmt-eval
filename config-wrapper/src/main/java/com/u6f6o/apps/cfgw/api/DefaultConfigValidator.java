package com.u6f6o.apps.cfgw.api;

import com.google.common.collect.ImmutableMap;

public class DefaultConfigValidator implements ConfigValidator {
    @Override
    public boolean onInit(ImmutableMap<String, String> config) {
        return true;
    }

    @Override
    public boolean onUpdate(ImmutableMap<String, String> oldConfig, ImmutableMap<String, String> newConfig) {
        return true;
    }
}
