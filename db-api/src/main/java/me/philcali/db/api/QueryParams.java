package me.philcali.db.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryParams {
    public static class Builder {
        private int maxSize = DEFAULT_MAX_SIZE;
        private IPageKey token;
        private Map<String, ICondition> conditions = new ConcurrentHashMap<>();
        private Collation collation = Collation.ASCENDING;

        public QueryParams build() {
            return new QueryParams(this);
        }

        public Collation getCollation() {
            return collation;
        }

        public Map<String, ICondition> getConditions() {
            return conditions;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public IPageKey getToken() {
            return token;
        }

        public Builder withCollation(final Collation collation) {
            this.collation = collation;
            return this;
        }

        public Builder withConditions(final ICondition ... conditions) {
            return withConditions(Arrays.asList(conditions));
        }

        public Builder withConditions(final List<ICondition> conditions) {
            conditions.forEach(condition -> {
                this.conditions.put(condition.getAttribute(), condition);
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

    public static enum Collation {
        ASCENDING,
        DESCENDING;
    }

    public static final int DEFAULT_MAX_SIZE = 100;

    public static Builder builder() {
        return new Builder();
    }

    private final IPageKey token;
    private final int maxSize;
    private final Map<String, ICondition> conditions;
    private final Collation collation;

    public QueryParams(final Builder builder) {
        this.conditions = builder.getConditions();
        this.maxSize = builder.getMaxSize();
        this.token = builder.getToken();
        this.collation = builder.getCollation();
    }

    public Collation getCollation() {
        return collation;
    }

    public Map<String, ICondition> getConditions() {
        return conditions;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public IPageKey getToken() {
        return token;
    }
}
