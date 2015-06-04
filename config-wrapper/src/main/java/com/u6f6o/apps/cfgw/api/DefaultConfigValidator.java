package com.u6f6o.apps.cfgw.api;

import com.google.common.collect.ImmutableMap;

public class DefaultConfigValidator implements ConfigValidator {
    @Override
    public boolean isValidateOnInit(ImmutableMap<String, String> initialConfig) {
        return !initialConfig.isEmpty();
    }

    @Override
    public boolean isValidOnUpdate(ImmutableMap<String, String> cachedConfig, ImmutableMap<String, String> upToDateConfig) {
        return !upToDateConfig.isEmpty();
    }
}
