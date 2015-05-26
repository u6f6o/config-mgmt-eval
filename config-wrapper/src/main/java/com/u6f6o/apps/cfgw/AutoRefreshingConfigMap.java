package com.u6f6o.apps.cfgw;

import com.google.common.collect.ImmutableMap;
import com.u6f6o.apps.cfgw.provider.ConfigFetcher;
import com.u6f6o.apps.cfgw.util.TimeSpan;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class AutoRefreshingConfigMap extends ReadOnlyMap {
    private static final Logger LOGGER = Logger.getLogger(AutoRefreshingConfigMap.class);

    private static final TimeSpan MAX_STARTUP_TIME = TimeSpan.seconds(10);
    private static final TimeSpan REFRESH_PERIOD = TimeSpan.seconds(10);

    private final ConfigFetcher configFetcher;
    private final AtomicBoolean canRefresh;
    private final ScheduledExecutorService refreshExecutor;

    private volatile ImmutableMap<String, String> configCache;


    private AutoRefreshingConfigMap(ConfigFetcher configFetcher) {
        this.configFetcher = configFetcher;
        this.canRefresh = new AtomicBoolean(true);
        this.refreshExecutor = Executors.newScheduledThreadPool(2); // 1 x fetch + 1 x timeout
    }

    public static AutoRefreshingConfigMap newConsulCatalogue(ConfigFetcher configFetcher) {
        AutoRefreshingConfigMap result = new AutoRefreshingConfigMap(configFetcher);
        result.initializeConfig();
        result.scheduleConfigRefresh();
        return result;
    }


    /**
     * Load initial configuration settings during startup. In case it does not work, the application
     * is supposed to stop immediately!
     *
     * @throws java.lang.AssertionError in case of timeout or failing validation
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
     * REFRESH_PERIOD. In case the new configuration cannot be fetched, the old configuration
     * is kept.
     *
     * This method does not throw any exception but logs in case something goes wrong. Failed
     * refresh of the configuration properties must not harm the running application.
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
                        LOGGER.error("Cannot refresh configuration, old values are kept!", e);
                    } finally {
                        canRefresh.set(true);
                    }
                }
            }
        }, REFRESH_PERIOD.getSpan(), REFRESH_PERIOD.getSpan(), REFRESH_PERIOD.getUnit());
    }

    /**
     * Fetches the latest configuration.
     * @param timeout max wait time for the fetch operation
     * @return latest configuration
     */
    private Map<String, String> fetchWithTimeout(TimeSpan timeout) {
        Future<Map<String, String>> future = refreshExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                return configFetcher.fetchLatestConfig();
            }
        });
        try {
            Map<String, String> freshConfig = future.get(timeout.getSpan(), timeout.getUnit());
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
