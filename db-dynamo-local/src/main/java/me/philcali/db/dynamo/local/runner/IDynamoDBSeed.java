package me.philcali.db.dynamo.local.runner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

@FunctionalInterface
public interface IDynamoDBSeed {
    void accept(final AmazonDynamoDB client) throws Exception;
}
