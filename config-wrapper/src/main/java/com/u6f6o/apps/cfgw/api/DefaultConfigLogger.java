package com.u6f6o.apps.cfgw.api;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Map;

public class DefaultConfigLogger implements ConfigLogger {
    private static final Logger LOGGER = Logger.getLogger(DefaultConfigLogger.class);
    private static final Joiner JOINER = Joiner.on(",");
    private static final ImmutableMap<String, String> EMPTY_MAP = ImmutableMap.copyOf(
            Collections.<String, String>emptyMap());

    @Override
    public void logOnUpdate(ImmutableMap<String, String> cachedConfig, Map<String, String> upToDateConfig) {
        if (cachedConfig != null && upToDateConfig != null) {
            MapDifference<String, String> difference = Maps.difference(cachedConfig, upToDateConfig);
            if(!difference.areEqual()) {
                logKeys("Updated keys: ", difference.entriesDiffering());
                logKeys("Removed keys: ", difference.entriesOnlyOnLeft());
                logKeys("Added keys: ", difference.entriesOnlyOnRight());
            }
        }
    }

    @Override
    public void logOnInit(ImmutableMap<String, String> initialConfig) {
        logOnUpdate(EMPTY_MAP, initialConfig);
    }

    private void logKeys(String message, Map<?, ?> config) {
        if(!config.isEmpty()) {
            LOGGER.info(message + JOINER.join(config.keySet()));
        }
    }
}
