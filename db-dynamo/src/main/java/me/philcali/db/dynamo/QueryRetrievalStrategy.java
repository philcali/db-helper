package me.philcali.db.dynamo;

import static me.philcali.db.dynamo.TranslationUtils.buildLastKey;
import static me.philcali.db.dynamo.TranslationUtils.translateFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import me.philcali.db.api.ICondition;
import me.philcali.db.api.ICondition.Comparator;
import me.philcali.db.api.IPageKey;
import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryParams.Collation;
import me.philcali.db.api.QueryResult;

public class QueryRetrievalStrategy implements IRetrievalStrategy {
    public static class Builder {
        private String hashKey;
        private String rangeKey;
        private Map<String, Index> indexMap = new HashMap<>();
        private Map<String, String> rangeMap = new HashMap<>();
        private IRetrievalStrategy fallback;

        public QueryRetrievalStrategy build() {
            return new QueryRetrievalStrategy(this);
        }

        public Builder withFallback(final IRetrievalStrategy strategy) {
            this.fallback = strategy;
            return this;
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

        public Builder withIndexMap(final String hashKey, final String rangeKey, final Index index) {
            return withIndexMap(hashKey, index).withRangeMap(rangeKey, hashKey);
        }

        public Builder withRangeKey(final String rangeKey) {
            this.rangeKey = rangeKey;
            return this;
        }

        public Builder withRangeMap(final Map<String, String> rangeMap) {
            this.rangeMap = rangeMap;
            return this;
        }

        public Builder withRangeMap(final String rangeKey, final String hashKey) {
            this.rangeMap.put(rangeKey, hashKey);
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static QueryRetrievalStrategy fromTable(final Table table) {
        final Builder builder = builder();
        final TableDescription description = Optional.ofNullable(table.getDescription())
                .orElseGet(table::describe);
        description.getKeySchema().forEach(key -> {
            switch (key.getKeyType()) {
            case "HASH":
                builder.withHashKey(key.getAttributeName());
                break;
            default:
                builder.withRangeKey(key.getAttributeName());
            }
        });
        final BiConsumer<String, List<KeySchemaElement>> buildIndex = (indexName, keys) -> {
            final Map<String, String> temp = keys.stream()
                    .collect(Collectors.toMap(
                            key -> key.getKeyType(),
                            key -> key.getAttributeName()));
            builder.withIndexMap(temp.get("HASH"), table.getIndex(indexName));
            Optional.ofNullable(temp.get("RANGE")).ifPresent(range -> {
                builder.withRangeMap(range, temp.get("HASH"));
            });
        };
        Optional.ofNullable(description.getGlobalSecondaryIndexes()).ifPresent(is -> is.forEach(index -> {
            buildIndex.accept(index.getIndexName(), index.getKeySchema());
        }));
        Optional.ofNullable(description.getLocalSecondaryIndexes()).ifPresent(is -> is.forEach(index -> {
            buildIndex.accept(index.getIndexName(), index.getKeySchema());
        }));
        return builder.build();
    }

    private final String hashKey;
    private final String rangeKey;
    private final IRetrievalStrategy fallback;
    private final Map<String, Index> indexMap;
    private final Map<String, String> rangeMap;

    private QueryRetrievalStrategy(final Builder builder) {
        this.hashKey = builder.hashKey;
        this.rangeKey = builder.rangeKey;
        this.indexMap = builder.indexMap;
        this.rangeMap = builder.rangeMap;
        this.fallback = Optional.ofNullable(builder.fallback).orElseGet(ScanRetrievalStrategy::new);
    }

    @Override
    public QueryResult<Item> apply(final QueryParams params, final Table table) {
        final Optional<ICondition> indexHashKey = findExactMatchIndexField(params, indexMap);
        if (params.getConditions().containsKey(hashKey) || indexHashKey.isPresent()) {
            final NameMap names = new NameMap();
            final ValueMap values = new ValueMap();
            final StringJoiner keyJoiner = new StringJoiner(" AND ");
            final StringJoiner filterJoiner = new StringJoiner(" AND ");
            final AtomicInteger index = new AtomicInteger();
            final QuerySpec spec = new QuerySpec()
                    .withMaxPageSize(params.getMaxSize());
            final Optional<String> rangeField = indexHashKey
                    .map(indexHash -> Optional.ofNullable(rangeMap.get(indexHash.getAttribute())))
                    .orElseGet(() -> Optional.ofNullable(rangeKey));
            final Optional<ICondition> range = rangeField
                    .flatMap(r -> Optional.ofNullable(params.getConditions().get(r)));
            final ICondition key = indexHashKey.orElseGet(() -> params.getConditions().get(hashKey));
            keyJoiner.add(translateFilter(new StringBuilder(), index.incrementAndGet(), values, names, key));
            range.ifPresent(rangeFilter -> {
                keyJoiner.add(translateFilter(new StringBuilder(), index.incrementAndGet(), values, names, rangeFilter));
            });
            buildLastKey(params).ifPresent(spec::withExclusiveStartKey);
            params.getConditions().values().stream()
                    .filter(filter -> !filter.equals(key) && !range.filter(filter::equals).isPresent())
                    .forEach(filter -> {
                        filterJoiner.add(translateFilter(new StringBuilder(), index.incrementAndGet(), values, names, filter));
                    });
            spec.withScanIndexForward(params.getCollation() == Collation.ASCENDING);
            spec.withKeyConditionExpression(keyJoiner.toString())
                    .withValueMap(values)
                    .withNameMap(names);
            if (filterJoiner.length() > 0) {
                spec.withFilterExpression(filterJoiner.toString());
            }
            final ItemCollection<QueryOutcome> outcomes = indexHashKey
                    .map(filter -> indexMap.get(filter.getAttribute()))
                    .map(i -> i.query(spec))
                    .orElseGet(() -> table.query(spec));
            final List<Item> items = outcomes.firstPage().getLowLevelResult().getItems();
            final Optional<IPageKey> lastKey = Optional.ofNullable(outcomes.firstPage()
                    .getLowLevelResult()
                    .getQueryResult()
                    .getLastEvaluatedKey())
                    .map(PageKeyDynamo::new);
            return new QueryResult<>(lastKey, items, items.size() == params.getMaxSize());
        } else {
            return fallback.apply(params, table);
        }
    }

    private Optional<Map<String, String>> convertIndexMapToString(final Map<String, Index> indexes) {
        return Optional.ofNullable(indexes).map(index -> index.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> entry.getValue().getIndexName())));
    }

    @Override
    public boolean equals(final Object obj) {
        if (Objects.isNull(obj) || !(obj instanceof QueryRetrievalStrategy)) {
            return false;
        }
        final QueryRetrievalStrategy query = (QueryRetrievalStrategy) obj;
        return Objects.equals(fallback, query.fallback)
                && Objects.equals(hashKey, query.hashKey)
                && Objects.equals(convertIndexMapToString(indexMap), convertIndexMapToString(query.indexMap))
                && Objects.equals(rangeKey, query.rangeKey)
                && Objects.equals(rangeMap, query.rangeMap);
    }

    private Optional<ICondition> findExactMatchIndexField(final QueryParams params, final Map<String, Index> indexMap) {
        return params.getConditions().entrySet().stream()
                .filter(entry -> indexMap.containsKey(entry.getKey())
                        && entry.getValue().getComparator() == Comparator.EQUALS)
                .findFirst()
                .map(entry -> entry.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fallback, hashKey, rangeKey, indexMap, rangeMap);
    }
}
