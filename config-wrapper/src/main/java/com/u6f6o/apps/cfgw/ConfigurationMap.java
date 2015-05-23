package com.u6f6o.apps.cfgw;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigurationMap extends ReadOnlyMap {
    private static final long MAX_STARTUP_TIME = 1; // minute
    private static final long REFRESH_PERIOD = 1; // minute

    private final ConfigFetcher configFetcher;
    private final AtomicBoolean canRefresh;
    private final ScheduledExecutorService refreshExecutor;

    private volatile ImmutableMap<String, String> propsCache;


    private ConfigurationMap(ConfigFetcher configFetcher) {
        this.configFetcher = configFetcher;
        this.canRefresh = new AtomicBoolean(true);
        this.refreshExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public static ConfigurationMap newConsulCatalogue(ConfigFetcher configFetcher) {
        ConfigurationMap result = new ConfigurationMap(configFetcher);
        result.initializeConfig();
        result.scheduleConfigRefresh();
        return result;
    }


    /**
     * Supposed to be run during application startup. MAX_STARTUP_TIME indicates how long to
     * wait for the configuration to be loaded.
     *
     * @throws java.lang.IllegalStateException in case of any error or timeout exceeded
     */
    private void initializeConfig() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Map<String, String>> future = executor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                return configFetcher.fetchLatestConfig();
            }
        });

        try {
            Map<String, String> freshConfig = future.get(MAX_STARTUP_TIME, TimeUnit.MINUTES);
            propsCache = ImmutableMap.copyOf(freshConfig);
        } catch (TimeoutException e) {
            future.cancel(true);
        } catch (Exception e) {
            throw new IllegalStateException("Properties initialization failed, aborting.", e);
        } finally {
            try {
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * Registers a task that repeatedly refreshes the cached configuration by a given delay,
     * REFRESH_PERIOD. In case something goes wrong, the old values are kept.
     */
    private void scheduleConfigRefresh() {
        refreshExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (canRefresh.compareAndSet(true, false)) {
                    try {
                        Map<String, String> fetchedProps = configFetcher.fetchLatestConfig();
                        propsCache = ImmutableMap.copyOf(fetchedProps);
                    } catch (Exception e) {
                        // TODO do sth, ffs! Logging should be enough here
                        e.printStackTrace();
                    } finally {
                        canRefresh.set(true);
                    }
                }
            }
        }, REFRESH_PERIOD, REFRESH_PERIOD, TimeUnit.MINUTES);
    }


    @Override
    public int size() {
        return propsCache.size();
    }

    @Override
    public boolean isEmpty() {
        return propsCache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return propsCache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return propsCache.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return propsCache.get(key);
    }

    @Override
    public Set<String> keySet() {
        return propsCache.keySet();
    }

    @Override
    public Collection<String> values() {
        return propsCache.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return propsCache.entrySet();
    }
}
