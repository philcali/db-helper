package me.philcali.db.api;

import me.philcali.db.api.IFilter.Condition;

public final class Filters {
    public static class NamedFilter {
        private final String attribute;

        public NamedFilter(final String attribute) {
            this.attribute = attribute;
        }

        public IFilter equalsTo(final Object value) {
            return create(attribute, value, Condition.EQUALS);
        }

        public IFilter notEqualsTo(final Object value) {
            return create(attribute, value, Condition.NOT_EQUALS);
        }
    }

    public static NamedFilter attribute(final String attribute) {
        return new NamedFilter(attribute);
    }

    private static IFilter create(final String attribute, final Object value, final Condition condition) {
        return new IFilter() {
            @Override
            public String getAttribute() {
                return attribute;
            }

            @Override
            public Condition getCondition() {
                return condition;
            }

            @Override
            public Object getValue() {
                return value;
            }
        };
    }

    private Filters() {

    }
}
