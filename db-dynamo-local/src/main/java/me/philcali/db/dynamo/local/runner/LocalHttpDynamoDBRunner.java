package me.philcali.db.dynamo.local.runner;

import java.util.Arrays;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;

class LocalHttpDynamoDBRunner extends AbstractDynamoDBRunner<DynamoDBProxyServer> {
    private AmazonDynamoDB client;

    public LocalHttpDynamoDBRunner(final String[] args) {
        super(args);
    }

    @Override
    public AmazonDynamoDB getClient() {
        if (client == null) {
            int port = Arrays.stream(args)
                    .filter(arg -> arg.matches("\\d+"))
                    .findFirst()
                    .map(arg -> Integer.parseInt(arg))
                    .orElse(8000);
            client = AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new EndpointConfiguration("http://localhost:" + port, "us-west-2"))
                    .build();
        }
        return client;
    }

    @Override
    protected DynamoDBProxyServer init() throws Exception {
        DynamoDBProxyServer server = null;
        if (!Arrays.asList(args).contains("-clientOnly")) {
            server = ServerRunner.createServerFromCommandLineArgs(args);
            server.start();
        }
        return server;
    }

    @Override
    protected void teardown(final DynamoDBProxyServer resource) throws Exception {
        if (resource != null) {
            resource.stop();
        }
    }
}
