package me.philcali.db.dynamo;

import java.util.Optional;
import java.util.StringJoiner;

import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import me.philcali.db.api.ICondition;
import me.philcali.db.api.QueryParams;

final class TranslationUtils {
    public static Optional<PrimaryKey> buildLastKey(final QueryParams params) {
        return Optional.ofNullable(params.getToken()).map(token -> {
            final PrimaryKey primaryKey = new PrimaryKey();
            token.getKey().forEach((attribute, value) -> {
                primaryKey.addComponent(attribute, value);
            });
            return primaryKey;
        });
    }

    public static StringBuilder translateFilter(final StringBuilder expression, final int index,
            final ValueMap values, final NameMap names, final ICondition apiFilter) {
        names.with("#n" + index, apiFilter.getAttribute());
        Optional.ofNullable(apiFilter.getValues()).ifPresent(vs -> {
            for (int i = 0; i < vs.length; i++) {
                values.with(":v" + index + "_" + i, vs[0]);
            }
        });
        switch (apiFilter.getComparator()) {
        case EQUALS:
            return expression.append(String.format("#n%d = :v%d_0", index, index));
        case NOT_EQUALS:
            return expression.append(String.format("#n%d <> :v%d_0", index, index));
        case LESS_THAN:
            return expression.append(String.format("#n%d < :v%d_0", index, index));
        case LESS_THAN_EQUALS:
            return expression.append(String.format("#n%d <= :v%d_0", index, index));
        case GREATER_THAN:
            return expression.append(String.format("#n%d > :v%d_0", index, index));
        case GREATER_THAN_EQUALS:
            return expression.append(String.format("#n%d >= :v%d_0", index, index));
        case BETWEEN:
            return expression.append(String.format("#n%d BETWEEN :v%d_0 AND :v%d_1", index, index, index));
        case CONTAINS:
            return expression.append(String.format("contains(#n%d, :v%d_0)", index, index));
        case NOT_CONTAINS:
            return expression.append(String.format("NOT contains(#n%d, :v%d_0)", index, index));
        case EXISTS:
            return expression.append(String.format("attribute_exists(#n%d)", index));
        case NOT_EXISTS:
            return expression.append(String.format("attribute_not_exists(#n%d)", index));
        case IN:
            final StringJoiner joiner = new StringJoiner(", ");
            values.keySet().stream()
                    .filter(key -> key.startsWith(":v" + index + "_"))
                    .forEach(joiner::add);
            return expression.append(String.format("#n%d IN (%s)", index, joiner.toString()));
        case STARTS_WITH:
            return expression.append(String.format("begins_with(#n%d, :v%d_0)", index, index));
        default:
            throw new IllegalArgumentException("Filter condition does not support condition: "
                    + apiFilter.getComparator());
        }
    }

    private TranslationUtils() {
    }
}
