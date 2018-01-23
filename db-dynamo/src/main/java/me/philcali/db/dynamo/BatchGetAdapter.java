package me.philcali.db.dynamo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;

public class BatchGetAdapter implements Function<List<PrimaryKey>, List<Item>> {
    private static final int MAX_PAGE_SIZE = 100;

    private final String tableName;
    private final DynamoDB db;

    public BatchGetAdapter(final DynamoDB db, final String tableName) {
        this.tableName = tableName;
        this.db = db;
    }

    @Override
    public List<Item> apply(final List<PrimaryKey> primaryKeys) {
        final List<Item> rval = new ArrayList<>();
        final List<PrimaryKey> keys = new ArrayList<>();
        final Supplier<List<Item>> lazyGet = () -> {
            final TableKeysAndAttributes tableKeys = new TableKeysAndAttributes(tableName);
            keys.forEach(tableKeys::addPrimaryKey);
            return db.batchGetItem(tableKeys).getTableItems().get(tableName);
        };
        for (final PrimaryKey key : primaryKeys) {
            keys.add(key);
            if (keys.size() == MAX_PAGE_SIZE) {
                rval.addAll(lazyGet.get());
                keys.clear();
            }
        }
        if (!keys.isEmpty()) {
            rval.addAll(lazyGet.get());
        }
        return rval;
    }
}
