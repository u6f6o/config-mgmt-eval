package com.u6f6o.apps.cfgw;

import com.google.common.collect.ImmutableMap;


public interface ConfigValidator {
    boolean isValidateOnInit(ImmutableMap<String, String> initialConfig);
    boolean isValidOnUpdate(ImmutableMap<String, String> cachedConfig, ImmutableMap<String, String> upToDateConfig);
}
