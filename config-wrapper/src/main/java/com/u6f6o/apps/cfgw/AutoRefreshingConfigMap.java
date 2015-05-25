package com.u6f6o.apps.cfgw;

import com.google.common.collect.ImmutableMap;
import com.u6f6o.apps.cfgw.provider.ConfigFetcher;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO: consider using FutureTask
 * TODO: add timeout also for config update
 * TODO: proper error handling
 * TODO: logging
 */
public class AutoRefreshingConfigMap extends ReadOnlyMap {
    private static final long MAX_STARTUP_TIME = 100000; // sec
    private static final long REFRESH_PERIOD = 100000; // sec

    private final ConfigFetcher configFetcher;
    private final AtomicBoolean canRefresh;
    private final ExecutorService timeoutExecutor;
    private final ScheduledExecutorService refreshExecutor;

    private volatile ImmutableMap<String, String> configCache;


    private AutoRefreshingConfigMap(ConfigFetcher configFetcher) {
        this.configFetcher = configFetcher;
        this.canRefresh = new AtomicBoolean(true);
        this.timeoutExecutor = Executors.newSingleThreadExecutor();
        this.refreshExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public static AutoRefreshingConfigMap newConsulCatalogue(ConfigFetcher configFetcher) {
        AutoRefreshingConfigMap result = new AutoRefreshingConfigMap(configFetcher);
        result.initializeConfig();
        result.scheduleConfigRefresh();
        return result;
    }



    /**
     * Supposed to be run during application startup. MAX_STARTUP_TIME indicates how long to
     * wait for the configuration to be initially loaded.
     *
     * @throws java.lang.AssertionError in case configuration cannot be initially loaded
     */
    private void initializeConfig() {
        Map<String, String> freshConfig = fetchWithTimeout(MAX_STARTUP_TIME);
        if(!validateConfig(freshConfig)) {
            throw new AssertionError("Application config could not be initially loaded, aborting!");
        }
        publishNewConfig(freshConfig);
    }


    /**
     * Registers a task that repeatedly refreshes the cached configuration by a given delay,
     * REFRESH_PERIOD. In case something goes wrong, the old values are kept.
     */
    private void scheduleConfigRefresh() {
        refreshExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (canRefresh.compareAndSet(true, false)) {
                    try {
                        Map<String, String> freshConfig = fetchWithTimeout(REFRESH_PERIOD); // TODO WRONG
                        if (!validateConfig(freshConfig)) {
                            throw new IllegalStateException("Cannot refresh configuration, old values are kept!");
                        }
                        publishNewConfig(freshConfig);
                    } catch (Exception e) {
                        // TODO MEANINGFUL LOGGING
                        e.printStackTrace();
                    } finally {
                        canRefresh.set(true);
                    }
                }
            }
        }, REFRESH_PERIOD, REFRESH_PERIOD, TimeUnit.MILLISECONDS);
    }

    /**
     * BLOCKING!!!
     * @param timeout
     * @return
     */
    private Map<String, String> fetchWithTimeout(long timeout) {
        Future<Map<String, String>> future = timeoutExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                return configFetcher.fetchLatestConfig();
            }
        });
        try {
            Map<String, String> freshConfig = future.get(timeout, TimeUnit.MILLISECONDS);
            return freshConfig;
        } catch (Exception e) {
            // TODO LOGGING
            future.cancel(true);
            return null;
        }
    }

    // TODO make hookable
    private boolean validateConfig(Map<String, String> freshConfig) {
        return freshConfig != null && freshConfig.size() > 0;
    }

    private void publishNewConfig(Map<String, String> freshConfig) {
        configCache = ImmutableMap.copyOf(freshConfig);
    }

    @Override
    public int size() {
        return configCache.size();
    }

    @Override
    public boolean isEmpty() {
        return configCache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return configCache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return configCache.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return configCache.get(key);
    }

    @Override
    public Set<String> keySet() {
        return configCache.keySet();
    }

    @Override
    public Collection<String> values() {
        return configCache.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return configCache.entrySet();
    }
}
