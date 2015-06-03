package com.u6f6o.apps.cfgw.api;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.Map;

public class DefaultConfigLogger implements ConfigLogger {
    private static final Logger LOGGER = Logger.getLogger(DefaultConfigLogger.class);
    private static final Joiner JOINER = Joiner.on(",");

    @Override
    public void onUpdate(ImmutableMap<String, String> oldConfig, Map<String, String> newConfig) {
        if (oldConfig != null && newConfig!= null) {
            MapDifference<String, String> difference = Maps.difference(oldConfig, newConfig);
            if(!difference.areEqual()) {
                logKeys("Updated keys: ", difference.entriesDiffering());
                logKeys("Removed keys: ", difference.entriesOnlyOnLeft());
                logKeys("Added keys: ", difference.entriesOnlyOnRight());
            }
        }
    }

    private void logKeys(String message, Map<?, ?> keyAndValues) {
        if(!keyAndValues.isEmpty()) {
            LOGGER.info(message + JOINER.join(keyAndValues.keySet()));
        }
    }
}
