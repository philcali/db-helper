package me.philcali.db.dynamo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryFilter;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanFilter;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.internal.Filter;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;

import me.philcali.db.api.IFilter;
import me.philcali.db.api.IPageKey;
import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryResult;
import me.philcali.db.api.IFilter.Condition;

public class QueryAdapter implements Function<Table, QueryResult<Item>> {
    public static class Builder {
        private String hashKey;
        private QueryParams params;
        private Map<String, Index> indexMap = new HashMap<>();

        public QueryAdapter build() {
            return new QueryAdapter(this);
        }

        public String getHashKey() {
            return hashKey;
        }

        public Map<String, Index> getIndexMap() {
            return indexMap;
        }

        public QueryParams getQueryParams() {
            return params;
        }

        public Builder withHashKey(final String hashKey) {
            this.hashKey = hashKey;
            return this;
        }

        public Builder withIndexMap(final Map<String, Index> indexMap) {
            this.indexMap = indexMap;
            return this;
        }

        public Builder withIndexMap(final String field, final Index index) {
            this.indexMap.put(field, index);
            return this;
        }

        public Builder withQueryParams(final QueryParams params) {
            this.params = params;
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String hashKey;
    private final QueryParams params;
    private final Map<String, Index> indexMap;

    private QueryAdapter(final Builder builder) {
        this.hashKey = builder.getHashKey();
        this.params = builder.getQueryParams();
        this.indexMap = builder.getIndexMap();
    }

    @Override
    public QueryResult<Item> apply(final Table table) {
        final Optional<IFilter> indexHashKey = findExactMatchIndexField();
        final QueryResult<Item> rval;
        if (params.getFilters().containsKey(hashKey) || indexHashKey.isPresent()) {
            final QuerySpec spec = new QuerySpec()
                    .withMaxPageSize(params.getMaxSize());
            final IFilter key = indexHashKey.orElseGet(() -> params.getFilters().get(hashKey));
            spec.withHashKey(key.getAttribute(), key.getValue());
            buildLastKey().ifPresent(spec::withExclusiveStartKey);
            params.getFilters().values().stream().filter(filter -> !filter.equals(key)).forEach(filter -> {
                spec.withQueryFilters(translate(new QueryFilter(filter.getAttribute()), filter));
            });
            final ItemCollection<QueryOutcome> outcomes = indexHashKey
                    .map(filter -> indexMap.get(filter.getAttribute()))
                    .map(index -> index.query(spec))
                    .orElseGet(() -> table.query(spec));
            final Optional<IPageKey> token = Optional.ofNullable(outcomes.firstPage()
                    .getLowLevelResult()
                    .getQueryResult()
                    .getLastEvaluatedKey())
                    .map(PageKeyDynamo::new);
            rval = new QueryResult<>(token,
                    outcomes.firstPage().getLowLevelResult().getItems(),
                    outcomes.firstPage().size() == params.getMaxSize());
        } else {
            final ScanSpec spec = new ScanSpec()
                    .withMaxPageSize(params.getMaxSize());
            buildLastKey().ifPresent(spec::withExclusiveStartKey);
            params.getFilters().values().stream().forEach(filter -> {
                spec.withScanFilters(translate(new ScanFilter(filter.getAttribute()), filter));
            });
            final ItemCollection<ScanOutcome> outcomes = table.scan(spec);
            final Optional<IPageKey> token = Optional.ofNullable(outcomes.firstPage()
                    .getLowLevelResult()
                    .getScanResult()
                    .getLastEvaluatedKey())
                    .map(PageKeyDynamo::new);
            rval = new QueryResult<>(token,
                    outcomes.firstPage().getLowLevelResult().getItems(),
                    outcomes.firstPage().size() == params.getMaxSize());
        }
        return rval;
    }

    private Optional<PrimaryKey> buildLastKey() {
        return Optional.ofNullable(params.getToken()).map(token -> {
            final PrimaryKey primaryKey = new PrimaryKey();
            token.getKey().forEach((attribute, value) -> {
                primaryKey.addComponent(attribute, value);
            });
            return primaryKey;
        });
    }

    private Optional<IFilter> findExactMatchIndexField() {
        return params.getFilters().entrySet().stream()
                .filter(entry -> indexMap.containsKey(entry.getKey())
                        && entry.getValue().getCondition() == Condition.EQUALS)
                .findFirst()
                .map(entry -> entry.getValue());
    }

    private <T extends Filter<T>> T translate(final Filter<T> newFilter, final IFilter apiFilter) {
        switch (apiFilter.getCondition()) {
        case EQUALS:
            return newFilter.eq(apiFilter.getValue());
        default:
            return newFilter.ne(apiFilter.getValue());
        }
    }
}
