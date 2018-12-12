package me.philcali.db.api;

import java.util.HashMap;
import java.util.Map;

public class PageKey implements IPageKey {
    private final Map<String, Object> compositeKey;

    public PageKey() {
        this.compositeKey = new HashMap<>();
    }

    public PageKey addKey(final String key, final Object value) {
        compositeKey.put(key, value);
        return this;
    }

    public void setKey(final Map<String, Object> key) {
        this.compositeKey.putAll(key);
    }

    @Override
    public Map<String, Object> getKey() {
        return compositeKey;
    }
}
