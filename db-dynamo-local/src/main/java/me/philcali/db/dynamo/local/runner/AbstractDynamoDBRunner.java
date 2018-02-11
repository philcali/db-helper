package me.philcali.db.dynamo.local.runner;

import java.util.function.Consumer;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

abstract class AbstractDynamoDBRunner<T> implements IDynamoDBRunner {
    protected final String[] args;

    public AbstractDynamoDBRunner(final String[] args) {
        this.args = args;
    }

    @Override
    public void enclosure(final RunNotifier notifier, Description desc, Consumer<RunNotifier> thunk) {
        T resource = null;
        try {
            resource = init();
            thunk.accept(notifier);
        } catch (Exception pe) {
            notifier.fireTestFailure(new Failure(desc, pe));
        } finally {
            if (resource != null) {
                try {
                    teardown(resource);
                } catch (Exception e) {
                    notifier.fireTestFailure(new Failure(desc, e));
                }
            }
        }
    }

    protected abstract T init() throws Exception;

    protected abstract void teardown(T resource) throws Exception;
}
