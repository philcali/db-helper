package me.philcali.db.api;

public interface ILogicalWords<T extends ILogicalWords<T>> {
    IConditionExpression and(final T combined);

    IConditionExpression or(final T combined);
}
