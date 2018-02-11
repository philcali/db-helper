package me.philcali.db.api;

import me.philcali.db.api.ICondition.Comparator;

public final class Conditions {
    public static class NamedCondition {
        private final String attribute;

        public NamedCondition(final String attribute) {
            this.attribute = attribute;
        }

        public ICondition between(final Object value1, final Object value2) {
            return create(attribute, Comparator.BETWEEN, value1, value2);
        }

        public ICondition contains(final Object value) {
            return create(attribute, Comparator.CONTAINS, value);
        }

        public ICondition equalsTo(final Object value) {
            return create(attribute, Comparator.EQUALS, value);
        }

        public ICondition exists() {
            return create(attribute, Comparator.EXISTS);
        }

        public ICondition ge(final Object value) {
            return create(attribute, Comparator.GREATER_THAN_EQUALS, value);
        }

        public ICondition gt(final Object value) {
            return create(attribute, Comparator.GREATER_THAN, value);
        }

        public ICondition in(final Object...values) {
            return create(attribute, Comparator.IN, values);
        }

        public ICondition le(final Object value) {
            return create(attribute, Comparator.LESS_THAN_EQUALS, value);
        }

        public ICondition lt(final Object value) {
            return create(attribute, Comparator.LESS_THAN, value);
        }

        public ICondition notContains(final Object value) {
            return create(attribute, Comparator.NOT_CONTAINS, value);
        }

        public ICondition notEqualsTo(final Object value) {
            return create(attribute, Comparator.NOT_EQUALS, value);
        }

        public ICondition notExists() {
            return create(attribute, Comparator.NOT_EXISTS);
        }

        public ICondition startsWith(final Object value) {
            return create(attribute, Comparator.STARTS_WITH, value);
        }
    }

    public static NamedCondition attribute(final String attribute) {
        return new NamedCondition(attribute);
    }

    private static ICondition create(final String attribute, final Comparator condition, final Object...values) {
        return new ICondition() {
            @Override
            public String getAttribute() {
                return attribute;
            }

            @Override
            public Comparator getComparator() {
                return condition;
            }

            @Override
            public Object[] getValues() {
                return values;
            }
        };
    }

    private Conditions() {

    }
}
