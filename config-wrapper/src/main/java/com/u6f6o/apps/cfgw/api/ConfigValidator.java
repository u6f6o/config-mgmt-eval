package com.u6f6o.apps.cfgw.api;

import java.util.Map;


public interface ConfigValidator {
    boolean onUpdate(Map<String, String> oldConfig, Map<String, String> newConfig);
}
