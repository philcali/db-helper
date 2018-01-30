package me.philcali.db.api;

import me.philcali.db.api.IFilter.Condition;

public final class Filters {
    public static class NamedFilter {
        private final String attribute;

        public NamedFilter(final String attribute) {
            this.attribute = attribute;
        }

        public IFilter equalsTo(final Object value) {
            return create(attribute, Condition.EQUALS, value);
        }

        public IFilter exists() {
            return create(attribute, Condition.EXISTS);
        }

        public IFilter notExists() {
            return create(attribute, Condition.NOT_EXISTS);
        }

        public IFilter startsWith(final Object value) {
            return create(attribute, Condition.STARTS_WITH, value);
        }

        public IFilter contains(final Object value) {
            return create(attribute, Condition.CONTAINS, value);
        }

        public IFilter notContains(final Object value) {
            return create(attribute, Condition.NOT_CONTAINS, value);
        }

        public IFilter in(final Object...values) {
            return create(attribute, Condition.IN, values);
        }

        public IFilter between(final Object value1, final Object value2) {
            return create(attribute, Condition.BETWEEN, value1, value2);
        }

        public IFilter notEqualsTo(final Object value) {
            return create(attribute, Condition.NOT_EQUALS, value);
        }
    }

    public static NamedFilter attribute(final String attribute) {
        return new NamedFilter(attribute);
    }

    private static IFilter create(final String attribute, final Condition condition, final Object...values) {
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
            public Object[] getValues() {
                return values;
            }
        };
    }

    private Filters() {

    }
}
