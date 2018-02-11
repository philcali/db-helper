package me.philcali.db.dynamo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import me.philcali.db.api.Conditions;
import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryResult;
import me.philcali.db.dynamo.local.runner.DynamoDBArgs;
import me.philcali.db.dynamo.local.runner.DynamoDBLocalRunner;
import me.philcali.db.dynamo.local.runner.DynamoDBSeed;
import me.philcali.db.dynamo.local.runner.DynamoDBTestClient;

@DynamoDBArgs({ "-clientOnly", "-port", "8001" })
@DynamoDBSeed(TestDataSeed.class)
@RunWith(DynamoDBLocalRunner.class)
public class QueryRetrievalStrategyTest {
    @DynamoDBTestClient
    private AmazonDynamoDB client;
    private DynamoDB db;
    private Table table;
    private IRetrievalStrategy query;

    @Before
    public void setUp() {
        db = new DynamoDB(client);
        table = db.getTable("TestDataSeed");
        query = QueryRetrievalStrategy.builder()
                .withHashKey("id")
                .withRangeKey("updateTime")
                .withIndexMap("name", table.getIndex("name-index"))
                .withIndexMap("race", "age", table.getIndex("age-index"))
                .build();
    }

    @Test
    public void testBuildFromTable() {
        assertEquals(query, QueryRetrievalStrategy.fromTable(table));
    }

    @Test
    public void testComplexFieldSearch() {
        final QueryParams params = QueryParams.builder()
                .withConditions(Conditions.attribute("origin.place").equalsTo("Shire"))
                .withConditions(Conditions.attribute("age").lt(50))
                .build();
        QueryResult<Item> results = query.apply(params, table);
        assertEquals(1, results.getItems().size());
        assertEquals("Frodo Baggins", results.getItems().get(0).getString("name"));
    }

    @Test
    public void testQueryNameIndex() {
        final QueryParams params = QueryParams.builder()
                .withConditions(Conditions.attribute("name").equalsTo("Philip Cali"))
                .build();
        QueryResult<Item> results = query.apply(params, table);
        assertEquals(1, results.getItems().size());
        assertFalse(results.isTruncated());
    }

    @Test
    public void testScan() {
        assertEquals(3, query.apply(QueryParams.builder().build(), table).getItems().size());
    }

    @Test
    public void testScanWithFilters() {
        final QueryParams params = QueryParams.builder()
                .withMaxSize(1)
                .withConditions(Conditions.attribute("race").equalsTo("hobbit"))
                .build();
        QueryResult<Item> results = query.apply(params, table);
        assertEquals(1, results.getItems().size());
        assertTrue(results.getToken().isPresent());
    }
}
