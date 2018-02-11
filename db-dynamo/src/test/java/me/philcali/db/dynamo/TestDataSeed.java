package me.philcali.db.dynamo;

import java.util.Arrays;
import java.util.stream.StreamSupport;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.philcali.db.dynamo.local.runner.IDynamoDBSeed;

public class TestDataSeed implements IDynamoDBSeed {
    private static final String TABLE_NAME = "TestDataSeed";
    private static final String TEST_DATA = "/test_seed_data.json";

    @Override
    public void accept(final AmazonDynamoDB client) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode nodes = mapper.readTree(getClass().getResourceAsStream(TEST_DATA));
        final TableDescription description = createTable(client);
        final DynamoDB ddb = new DynamoDB(client);
        final Table table = ddb.getTable(description.getTableName());
        StreamSupport.stream(nodes.spliterator(), false).forEach(node -> {
            table.putItem(Item.fromJSON(node.toString()));
        });
    }


    private TableDescription createTable(final AmazonDynamoDB client) {
        GlobalSecondaryIndex nameIndex = new GlobalSecondaryIndex()
                .withIndexName("name-index")
                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(10L)
                        .withWriteCapacityUnits(10L))
                .withKeySchema(
                        new KeySchemaElement()
                                .withAttributeName("name")
                                .withKeyType(KeyType.HASH));
        GlobalSecondaryIndex ageIndex = new GlobalSecondaryIndex()
                .withIndexName("age-index")
                .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(10L)
                        .withWriteCapacityUnits(10L))
                .withKeySchema(
                        new KeySchemaElement()
                                .withAttributeName("race")
                                .withKeyType(KeyType.HASH),
                        new KeySchemaElement()
                                .withAttributeName("age")
                                .withKeyType(KeyType.RANGE));
        try {
            return client.createTable(new CreateTableRequest()
                    .withTableName(TABLE_NAME)
                    .withGlobalSecondaryIndexes(Arrays.asList(nameIndex, ageIndex))
                    .withAttributeDefinitions(
                            new AttributeDefinition()
                                    .withAttributeName("id")
                                    .withAttributeType(ScalarAttributeType.S),
                            new AttributeDefinition()
                                    .withAttributeName("updateTime")
                                    .withAttributeType(ScalarAttributeType.N),
                            new AttributeDefinition()
                                    .withAttributeName("race")
                                    .withAttributeType(ScalarAttributeType.S),
                            new AttributeDefinition()
                                    .withAttributeName("name")
                                    .withAttributeType(ScalarAttributeType.S),
                            new AttributeDefinition()
                                    .withAttributeName("age")
                                    .withAttributeType(ScalarAttributeType.N))
                    .withProvisionedThroughput(new ProvisionedThroughput()
                                .withReadCapacityUnits(15L)
                                .withWriteCapacityUnits(15L))
                    .withKeySchema(
                            new KeySchemaElement()
                                    .withAttributeName("id")
                                    .withKeyType(KeyType.HASH),
                            new KeySchemaElement()
                                    .withAttributeName("updateTime")
                                    .withKeyType(KeyType.RANGE)))
                    .getTableDescription();
        } catch (ResourceInUseException riuse) {
            return client.describeTable(new DescribeTableRequest(TABLE_NAME)).getTable();
        }
    }
}
