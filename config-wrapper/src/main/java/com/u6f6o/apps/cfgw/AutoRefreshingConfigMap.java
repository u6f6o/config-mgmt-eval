package com.u6f6o.apps.cfgw;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.u6f6o.apps.cfgw.api.ConfigFetcher;
import com.u6f6o.apps.cfgw.api.ConfigLogger;
import com.u6f6o.apps.cfgw.api.ConfigValidator;
import com.u6f6o.apps.cfgw.api.DefaultConfigLogger;
import com.u6f6o.apps.cfgw.api.DefaultConfigValidator;
import com.u6f6o.apps.cfgw.api.ReadOnlyMap;
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

public class AutoRefreshingConfigMap extends ReadOnlyMap {
    private static final Logger LOGGER = Logger.getLogger(AutoRefreshingConfigMap.class);

    private static final TimeSpan DEFAULT_MAX_STARTUP_TIME = TimeSpan.seconds(5l);
    private static final TimeSpan DEFAULT_REFRESH_PERIOD = TimeSpan.seconds(5l);

    private final ConfigFetcher configFetcher;
    private final ConfigValidator configValidator = new DefaultConfigValidator();
    private final ConfigLogger configLogger = new DefaultConfigLogger();

    private final TimeSpan maxStartupTime;
    private final TimeSpan refreshPeriod;

    private final AtomicBoolean canRefresh = new AtomicBoolean(true);
    private final ScheduledExecutorService refreshExecutor = Executors.newScheduledThreadPool(2);

    private volatile ImmutableMap<String, String> configCache;


    private AutoRefreshingConfigMap(ConfigFetcher configFetcher) {
        this(configFetcher, DEFAULT_MAX_STARTUP_TIME, DEFAULT_REFRESH_PERIOD);
    }

    private AutoRefreshingConfigMap( ConfigFetcher configFetcher,
                                     TimeSpan maxStartupTime,
                                     TimeSpan refreshPeriod) {
        this.configFetcher = configFetcher;
        this.maxStartupTime = maxStartupTime;
        this.refreshPeriod = refreshPeriod;
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
        Map<String, String> freshConfig = fetchWithTimeout(maxStartupTime);
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
                        // TODO looks really ugly
                        Map<String, String> recentConfig = fetchWithTimeout(maxStartupTime);
                        if (recentConfig == null) {
                            throw new IllegalStateException("Received null config, aborting update");
                        }
                        ImmutableMap<String, String> config = ImmutableMap.copyOf(recentConfig);
                        if (!configValidator.onUpdate(configCache, config)) {
                            throw new IllegalStateException("Validation failed, aborting update");
                        }
                        configLogger.onUpdate(configCache, config);
                        publishNewConfig(config);
                    } catch (Exception e) {
                        LOGGER.error("Cannot refresh configuration, old values are kept!", e);
                    } finally {
                        canRefresh.set(true);
                    }
                }
            }
        }, refreshPeriod.getSpan(), refreshPeriod.getSpan(), refreshPeriod.getUnit());
    }

    /**
     * Fetches the latest configuration.
     * @param timeout max wait time for the fetch operation
     * @return latest configuration or null in case of error/timeout
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
            LOGGER.info("Unable to fetch configuration", e);
            future.cancel(true);
            return null;
        }
    }

    private void publishNewConfig(ImmutableMap<String, String> freshConfig) {
        configCache = freshConfig;
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
