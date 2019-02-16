package me.philcali.db.processor.example;

import java.util.stream.StreamSupport;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.philcali.db.dynamo.local.runner.IDynamoDBSeed;

public class TestDataSeed implements IDynamoDBSeed {
    private static final String TABLE_NAME = "GeneratedPeople";
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
        try {
            return client.createTable(new CreateTableRequest()
                    .withTableName(TABLE_NAME)
                    .withAttributeDefinitions(
                            new AttributeDefinition()
                            .withAttributeName("name")
                            .withAttributeType(ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(15L)
                            .withWriteCapacityUnits(15L))
                    .withKeySchema(
                            new KeySchemaElement()
                            .withAttributeName("name")
                            .withKeyType(KeyType.HASH)))
                    .getTableDescription();
        } catch (ResourceInUseException riuse) {
            return client.describeTable(new DescribeTableRequest(TABLE_NAME)).getTable();
        }
    }
}
