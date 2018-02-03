package me.philcali.db.dynamo;

import java.util.function.BiFunction;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryResult;

public interface IRetrievalStrategy extends BiFunction<QueryParams, Table, QueryResult<Item>> {
    @Override
    QueryResult<Item> apply(QueryParams params, Table table);
}
