package me.philcali.db.dynamo.local.runner;

import java.util.function.Consumer;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

interface IDynamoDBRunner {
    void enclosure(RunNotifier notifier, Description desc, Consumer<RunNotifier> thunk);

    AmazonDynamoDB getClient();
}
