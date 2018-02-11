package me.philcali.db.dynamo;

import static me.philcali.db.dynamo.TranslationUtils.buildLastKey;
import static me.philcali.db.dynamo.TranslationUtils.translateFilter;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import me.philcali.db.api.ICondition;
import me.philcali.db.api.IPageKey;
import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryResult;

public final class ScanRetrievalStrategy implements IRetrievalStrategy {
    @Override
    public QueryResult<Item> apply(final QueryParams params, final Table table) {
        final ScanSpec spec = new ScanSpec()
                .withMaxPageSize(params.getMaxSize());
        buildLastKey(params).ifPresent(spec::withExclusiveStartKey);
        final StringJoiner joiner = new StringJoiner(" AND ");
        final ValueMap values = new ValueMap();
        final NameMap names = new NameMap();
        final Iterator<ICondition> filters = params.getConditions().values().iterator();
        final AtomicInteger index = new AtomicInteger();
        while (filters.hasNext()) {
            final ICondition filter = filters.next();
            joiner.add(translateFilter(new StringBuilder(), index.incrementAndGet(), values, names, filter));
        }
        if (!values.isEmpty() && !names.isEmpty()) {
            spec.withFilterExpression(joiner.toString())
                    .withNameMap(names)
                    .withValueMap(values);
        }
        final ItemCollection<ScanOutcome> outcomes = table.scan(spec);
        final List<Item> items = outcomes.firstPage().getLowLevelResult().getItems();
        final Optional<IPageKey> pageKey = Optional.ofNullable(outcomes.firstPage()
                .getLowLevelResult()
                .getScanResult()
                .getLastEvaluatedKey())
                .map(PageKeyDynamo::new);
        return new QueryResult<>(pageKey, items, items.size() == params.getMaxSize());
    }

    @Override
    public boolean equals(final Object obj) {
        if (Objects.isNull(obj) || !(obj instanceof ScanRetrievalStrategy)) {
            return false;
        }
        return obj instanceof ScanRetrievalStrategy;
    }
}
