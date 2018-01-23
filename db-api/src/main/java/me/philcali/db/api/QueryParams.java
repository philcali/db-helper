package me.philcali.db.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryParams {
    public static class Builder {
        private int maxSize = DEFAULT_MAX_SIZE;
        private IPageKey token;
        private Map<String, IFilter> filters = new ConcurrentHashMap<>();

        public QueryParams build() {
            return new QueryParams(this);
        }

        public Map<String, IFilter> getFilters() {
            return filters;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public IPageKey getToken() {
            return token;
        }

        public Builder withFilters(final IFilter ... filters) {
            return withFilters(Arrays.asList(filters));
        }

        public Builder withFilters(final List<IFilter> filters) {
            filters.forEach(filter -> {
                this.filters.put(filter.getAttribute(), filter);
            });
            return this;
        }

        public Builder withMaxSize(final int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder withToken(final IPageKey token) {
            this.token = token;
            return this;
        }
    }

    public static final int DEFAULT_MAX_SIZE = 100;

    public static Builder builder() {
        return new Builder();
    }

    private final IPageKey token;
    private final int maxSize;
    private final Map<String, IFilter> filters;

    public QueryParams(final Builder builder) {
        this.filters = builder.getFilters();
        this.maxSize = builder.getMaxSize();
        this.token = builder.getToken();
    }

    public Map<String, IFilter> getFilters() {
        return filters;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public IPageKey getToken() {
        return token;
    }
}
