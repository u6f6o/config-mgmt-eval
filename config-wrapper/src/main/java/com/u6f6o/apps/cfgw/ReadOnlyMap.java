package com.u6f6o.apps.cfgw;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public abstract class ReadOnlyMap implements Map<String, String> {

    @Override
    @Deprecated
    public final String put(String key, String value) {
        throw new UnsupportedOperationException("This map is read only!");
    }

    @Override
    @Deprecated
    public final String remove(Object key) {
        throw new UnsupportedOperationException("This map is read only!");
    }

    @Override
    @Deprecated
    public final void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("This map is read only!");
    }

    @Override
    @Deprecated
    public final void clear() {
        throw new UnsupportedOperationException("This map is read only!");
    }
}
