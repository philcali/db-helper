package me.philcali.db.dynamo.local.runner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

class SwitchDynamoDBLocal implements IDynamoDBRunner {
    private IDynamoDBRunner runner;

    public SwitchDynamoDBLocal(final String[] args) {
        final Set<String> keys = new HashSet<>(Arrays.asList(args));
        if (keys.contains("-embedded")) {
            runner = new EmbeddedDynamoDBRunner(args);
        } else {
            runner = new LocalHttpDynamoDBRunner(args);
        }
    }

    @Override
    public void enclosure(RunNotifier notifier, Description desc, Consumer<RunNotifier> thunk) {
        runner.enclosure(notifier, desc, thunk);
    }

    @Override
    public AmazonDynamoDB getClient() {
        return runner.getClient();
    }

}
