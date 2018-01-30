package me.philcali.db.api;

public interface IFilter {
    static enum Condition {
        EQUALS,
        NOT_EQUALS,
        STARTS_WITH,
        EXISTS,
        NOT_EXISTS,
        BETWEEN,
        CONTAINS,
        NOT_CONTAINS,
        IN,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_EQUALS,
        LESS_THAN_EQUALS;
    }

    String getAttribute();
    Condition getCondition();
    Object[] getValues();

    default Object getValue() {
        return getValues()[0];
    }
}
