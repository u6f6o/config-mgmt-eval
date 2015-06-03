package com.u6f6o.apps.cfgw.api;

import java.util.Map;

public class DefaultConfigValidator implements ConfigValidator {
    @Override
    public boolean onUpdate(Map<String, String> oldConfig, Map<String, String> newConfig) {
        return true;
    }
}
