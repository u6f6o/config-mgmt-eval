package com.u6f6o.apps.cfgw;

import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;


public final class AutoRefreshingConfigMap extends ReadOnlyMap {
    private static final Logger LOGGER = Logger.getLogger(AutoRefreshingConfigMap.class);

    private static final TimeSpan DEFAULT_MAX_STARTUP_TIME = TimeSpan.seconds(30l);
    private static final TimeSpan DEFAULT_REFRESH_PERIOD = TimeSpan.seconds(30l);

    private final AtomicBoolean canRefresh = new AtomicBoolean(true);
    private final ScheduledExecutorService refreshExecutor = Executors.newScheduledThreadPool(2);

    private final ConfigFetcher configFetcher;
    private final ConfigValidator configValidator = new DefaultConfigValidator();
    private final ConfigLogger configLogger = new DefaultConfigLogger();
    private final TimeSpan maxStartupTime;
    private final TimeSpan refreshPeriod;

    // replaced on update in different thread
    private volatile ImmutableMap<String, String> cachedConfig;


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
     * @throws ApplicationBootstrapError in case of timeout or failing validation
     */
    private void initializeConfig() {
        ImmutableMap<String, String> initialConfig = ImmutableMap.copyOf(
                fetchWithTimeout(maxStartupTime));

        if (!configValidator.isValidateOnInit(initialConfig)) {
            throw new ApplicationBootstrapError("Initial configuration fetch failed.");
        }
        configLogger.logOnInit(initialConfig);
        publishNewConfig(initialConfig);
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
                        ImmutableMap<String, String> upToDateConfig = ImmutableMap.copyOf(
                                fetchWithTimeout(maxStartupTime));

                        if (!configValidator.isValidOnUpdate(cachedConfig, upToDateConfig)) {
                            throw new IllegalStateException("Fetched configuration is invalid.");
                        }
                        configLogger.logOnUpdate(cachedConfig, upToDateConfig);
                        publishNewConfig(upToDateConfig);
                    } catch (Exception e) {
                        LOGGER.error("Configuration refresh failed.", e);
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
            return future.get(timeout.getSpan(), timeout.getUnit());
        } catch (Exception e) {
            LOGGER.info("Unable to fetch configuration", e);
            future.cancel(true);
            return Collections.emptyMap();
        }
    }

    private void publishNewConfig(ImmutableMap<String, String> freshConfig) {
        cachedConfig = freshConfig;
    }

    @Override
    public int size() {
        return cachedConfig.size();
    }

    @Override
    public boolean isEmpty() {
        return cachedConfig.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cachedConfig.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cachedConfig.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return cachedConfig.get(key);
    }

    @Override
    public Set<String> keySet() {
        return cachedConfig.keySet();
    }

    @Override
    public Collection<String> values() {
        return cachedConfig.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return cachedConfig.entrySet();
    }
}
