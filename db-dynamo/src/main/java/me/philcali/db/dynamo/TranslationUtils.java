package me.philcali.db.dynamo;

import java.util.Arrays;
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
        final StringJoiner attributeNameParts = new StringJoiner(".");
        Arrays.stream(apiFilter.getAttribute().split("\\.")).forEach(part -> {
            final String namePart = "#" + part + index;
            names.with(namePart, part);
            attributeNameParts.add(namePart);
        });
        final String attributeName = attributeNameParts.toString();
        Optional.ofNullable(apiFilter.getValues()).ifPresent(vs -> {
            for (int i = 0; i < vs.length; i++) {
                values.with(":v" + index + "_" + i, vs[0]);
            }
        });
        switch (apiFilter.getComparator()) {
        case EQUALS:
            return expression.append(String.format("%s = :v%d_0", attributeName, index));
        case NOT_EQUALS:
            return expression.append(String.format("%s <> :v%d_0", attributeName, index));
        case LESS_THAN:
            return expression.append(String.format("%s < :v%d_0", attributeName, index));
        case LESS_THAN_EQUALS:
            return expression.append(String.format("%s <= :v%d_0", attributeName, index));
        case GREATER_THAN:
            return expression.append(String.format("%s > :v%d_0", attributeName, index));
        case GREATER_THAN_EQUALS:
            return expression.append(String.format("%s >= :v%d_0", attributeName, index));
        case BETWEEN:
            return expression.append(String.format("%s BETWEEN :v%d_0 AND :v%d_1", attributeName, index, index));
        case CONTAINS:
            return expression.append(String.format("contains(%s, :v%d_0)", attributeName, index));
        case NOT_CONTAINS:
            return expression.append(String.format("NOT contains(%s, :v%d_0)", attributeName, index));
        case EXISTS:
            return expression.append(String.format("attribute_exists(%s)", attributeName));
        case NOT_EXISTS:
            return expression.append(String.format("attribute_not_exists(%s)", attributeName));
        case IN:
            final StringJoiner joiner = new StringJoiner(", ");
            values.keySet().stream()
                    .filter(key -> key.startsWith(":v" + index + "_"))
                    .forEach(joiner::add);
            return expression.append(String.format("%s IN (%s)", attributeName, joiner.toString()));
        case STARTS_WITH:
            return expression.append(String.format("begins_with(%s, :v%d_0)", attributeName, index));
        default:
            throw new IllegalArgumentException("Filter condition does not support condition: "
                    + apiFilter.getComparator());
        }
    }

    private TranslationUtils() {
    }
}
