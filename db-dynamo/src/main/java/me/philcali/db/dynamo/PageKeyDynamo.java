package me.philcali.db.dynamo;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import me.philcali.db.api.IPageKey;

public class PageKeyDynamo implements IPageKey {
    private final Map<String, AttributeValue> lastKey;

    public PageKeyDynamo(final Map<String, AttributeValue> lastKey) {
        this.lastKey = lastKey;
    }

    @Override
    public Map<String, Object> getKey() {
        return lastKey.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> getPrimaryKeyValue(entry.getValue())));
    }

    private Object getPrimaryKeyValue(final AttributeValue value) {
        return Optional.ofNullable(value.getS()).orElseGet(value::getN);
    }
}
