package me.philcali.db.dynamo;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;

public class BatchGetAdapterTest {
    private BatchGetAdapter adapter;
    private DynamoDB db;

    @Before
    public void setUp() {
        db = mock(DynamoDB.class);
        adapter = new BatchGetAdapter(db, "testTable");
    }

    @Test
    public void testApply() {
        final List<List<PrimaryKey>> attempts = new ArrayList<>();
        attempts.add(new ArrayList<>());
        attempts.add(new ArrayList<>());
        final List<List<Item>> items = new ArrayList<>();
        items.add(new ArrayList<>());
        items.add(new ArrayList<>());
        final List<PrimaryKey> keys = new ArrayList<>();
        for (int index = 0; index <= 120; index++) {
            final PrimaryKey key = new PrimaryKey().addComponent("id", index);
            final Item item = new Item().withString("id", "item" + index);
            attempts.get(index < 100 ? 0 : 1).add(key);
            items.get(index < 100 ? 0 : 1).add(item);
        }
        attempts.forEach(keys::addAll);
        final AtomicInteger counter = new AtomicInteger();
        when(db.batchGetItem(any(TableKeysAndAttributes.class))).then(invoke -> {
            final TableKeysAndAttributes attributes = invoke.getArgumentAt(0, TableKeysAndAttributes.class);
            final BatchGetItemResult result = new BatchGetItemResult();
            final BatchGetItemOutcome outcome = new BatchGetItemOutcome(result);
            assertEquals(attempts.get(counter.get()), attributes.getPrimaryKeys());
            result.addResponsesEntry("testTable", items.get(counter.getAndIncrement()).stream()
                    .map(item -> {
                        final Map<String, AttributeValue> values = new HashMap<>();
                        values.put("id", new AttributeValue(item.getString("id")));
                        return values;
                    })
                    .collect(Collectors.toList()));
            return outcome;
        });
        List<Item> allItems = new ArrayList<>();
        items.forEach(allItems::addAll);
        assertEquals(allItems, adapter.apply(keys));
    }

}
