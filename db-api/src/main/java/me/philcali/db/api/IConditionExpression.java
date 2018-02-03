package me.philcali.db.api;

// TODO: make this actually work
public interface IConditionExpression extends ICondition, ILogicalWords<IConditionExpression> {
    enum Logical {
        AND,
        OR,
        NOT;
    }

    @Override
    default IConditionExpression and(final IConditionExpression combined) {
        return new IConditionExpression() {
            @Override
            public ICondition[] getConditions() {
                return null;
            }

            @Override
            public IConditionExpression[] getExpressions() {
                return new IConditionExpression[] { IConditionExpression.this, combined };
            }

            @Override
            public Logical getLogicalEvaluation() {
                return Logical.AND;
            }
        };
    }

    @Override
    default String getAttribute() {
        return null;
    }

    @Override
    default Comparator getComparator() {
        return null;
    }

    ICondition[] getConditions();

    IConditionExpression[] getExpressions();

    Logical getLogicalEvaluation();

    @Override
    default Object[] getValues() {
        return null;
    }

    @Override
    default IConditionExpression or(final IConditionExpression combined) {
        return new IConditionExpression() {
            @Override
            public ICondition[] getConditions() {
                return null;
            }

            @Override
            public IConditionExpression[] getExpressions() {
                return new IConditionExpression[] { IConditionExpression.this, combined };
            }

            @Override
            public Logical getLogicalEvaluation() {
                return Logical.OR;
            }
        };
    }
}
