package com.u6f6o.apps.cfgw;

import java.util.concurrent.TimeUnit;

public class TimeSpan {
    private final TimeUnit unit;
    private final long time;

    private TimeSpan(TimeUnit unit, long time) {
        this.unit = unit;
        this.time = time;
    }

    public static TimeSpan millis (long time) {
        return new TimeSpan(TimeUnit.MILLISECONDS, time);
    }

    public static TimeSpan seconds(long time) {
        return new TimeSpan(TimeUnit.SECONDS, time);
    }

    public static TimeSpan minutes(long time) {
        return new TimeSpan(TimeUnit.MINUTES, time);
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public long getSpan() {
        return time;
    }

    @Override
    public String toString() {
        return "TimeSpan{" +
                "unit=" + unit +
                ", time=" + time +
                '}';
    }
}
