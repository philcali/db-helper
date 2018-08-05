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

    @Override
    public Map<String, Object> getKey() {
        return compositeKey;
    }
}
