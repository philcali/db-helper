package me.philcali.db.api;

public interface IFilter {
    static enum Condition {
        EQUALS,
        NOT_EQUALS;
    }

    String getAttribute();
    Condition getCondition();
    Object getValue();
}
