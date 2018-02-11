package me.philcali.db.dynamo.local.runner;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;

public class DynamoDBLocalRunner extends BlockJUnit4ClassRunner {
    private final IDynamoDBRunner runner;

    public DynamoDBLocalRunner(final Class<?> klass) throws InitializationError {
        super(klass);
        runner = new SwitchDynamoDBLocal(findDynamoDBArgs());
    }

    @Override
    protected Object createTest() throws Exception {
        final Object test = super.createTest();
        for (FrameworkField field : getTestClass().getAnnotatedFields(DynamoDBTestClient.class)) {
            final Field javaField = field.getField();
            javaField.setAccessible(true);
            javaField.set(test, runner.getClient());
        }
        Optional.ofNullable(getTestClass().getAnnotation(DynamoDBSeed.class))
                .ifPresent(this::runSeeds);
        return test;
    }

    private String[] findDynamoDBArgs() {
        final DynamoDBArgs args = getTestClass().getAnnotation(DynamoDBArgs.class);
        return Optional.ofNullable(args)
                .map(a -> a.value())
                .filter(as -> as.length > 0)
                .orElseGet(() -> new String[] { "-clientOnly" });
    }

    private boolean ignoreRunner() {
        return Optional.ofNullable(getTestClass().getAnnotation(DynamoDBArgs.class))
                .filter(args -> args.skipIfUnavailable())
                .isPresent();
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        Optional.ofNullable(method.getAnnotation(DynamoDBSeed.class))
                .ifPresent(this::runSeeds);
        return super.methodInvoker(method, test);
    }

    @Override
    public void run(final RunNotifier notifier) {
        runner.enclosure(notifier, getDescription(), super::run);
    }

    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        final Optional<Throwable> validation = validateExternalEnvironment();
        if (validation.isPresent()) {
            if (ignoreRunner()) {
                notifier.fireTestIgnored(getDescription());
            } else {
                notifier.fireTestFailure(new Failure(getDescription(), validation.get()));
            }
        } else {
            super.runChild(method, notifier);
        }
    }

    private void runSeeds(final DynamoDBSeed seeds) {
        for (final Class<? extends IDynamoDBSeed> seedClass : seeds.value()) {
            try {
                final IDynamoDBSeed seed = seedClass.newInstance();
                seed.accept(runner.getClient());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Optional<Throwable> validateExternalEnvironment() {
        Throwable ex = null;
        try {
            runner.getClient().listTables(new ListTablesRequest()
                    .withSdkClientExecutionTimeout(validationTimeout())
                    .withSdkRequestTimeout(validationTimeout()));
        } catch (Throwable t) {
            ex = t;
        }
        return Optional.ofNullable(ex);
    }

    private int validationTimeout() {
        return Optional.ofNullable(getTestClass().getAnnotation(DynamoDBArgs.class))
                .map(args -> args.validationTimeout())
                .orElse(1000);
    }
}
