package me.philcali.db.dynamo.local.runner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;

class EmbeddedDynamoDBRunner extends AbstractDynamoDBRunner<AmazonDynamoDB> {
    private AmazonDynamoDB ddb;

    public EmbeddedDynamoDBRunner(final String args[]) {
        super(args);
    }

    @Override
    public AmazonDynamoDB getClient() {
        return ddb;
    }

    @Override
    protected AmazonDynamoDB init() throws Exception {
        ddb = DynamoDBEmbedded.create().amazonDynamoDB();
        return ddb;
    }

    @Override
    protected void teardown(AmazonDynamoDB resource) throws Exception {
        resource.shutdown();
    }

}
