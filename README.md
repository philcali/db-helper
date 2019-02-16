# DB helper

This is a collection of _no fuss_, light-weight database
adapters for repositories.

## QueryParams and QueryResult

A developer can provide list like queries
for their abstract datastore using `QueryParams` and `QueryResult`

```
public interface IEmployeeRepository {
    String EMPLOYEE_ID = "id";
    String COMPANY_ID = "companyId";

    Optional<IEmployee> get(String uuid);

    QueryResult<IEmployee> list(QueryParams params);

    default QueryResult<IEmployee> listByCompany(String companyId) {
        return list(QueryParams.builder()
            .withFilters(Filters.attribute(COMPANY_ID).equalsTo(companyId))
            .build());
    }
}
```

## DynamoDB Implementation

The DynamoDB implementation specifically adapts a set of `QueryParams` to
be run against a `Table`. Using the previous repo, one might define the
DynamoDB implementation as follows:

```
public class EmployeeRepositoryDynamo implements IEmployeeRepository {
    private static final String COMPANY_INDEX = "companyId-index";
    private final Table employees;

    public EmployeeRepositoryDynamo(final Table employees) {
        this.employees = employees;
    }

    @Override
    public Optional<IEmployee> get(final String uuid) {
        return Optional.ofNullable(employees.getItem(EMPLOYEE_ID, uuid))
                .map(EmployeeDynamo::new);
    }

    @Override
    public QueryResult<IEmployee> list(final QueryParams params) {
        final QueryAdapter adapter = QueryAdapter.builder()
                .withQueryParams(params)
                .withHashKeyField(EMPLOYEE_ID)
                .withIndexMap(COMPANY_ID, employees.getIndex(COMPANY_INDEX))
                .build();
        return adapter.andThen(result -> result.map(EmployeeDynamo::new)).apply(employees);
    }
}
```

Note the `QueryAdapter` bit. The intelligence behind this adapter will
correctly choose a table query or scan, index query or scan with any
additional filters correctly supplied. The result is filled with an
items list, and the last evaluated key for pagination.

## Auto Implementations

Sometimes it's really frustrating to handroll common implementations
for something like that dynamo. With the `db-dynamo-processor` you
no longer have to! Check out the `db-process-example` to get an idea
how this works.

``` java
@Repository(keys = @Key(partition = "name"))
@ExceptionTranslations({
    @ExceptionTranslation(source = SdkBaseException.class, destination = PersonStorageException.class)
})
public interface PersonRepository {
    @Repository.Method(Action.READ)
    Optional<Person> get(String name) throws PersonStorageException;

    QueryResult<Person> list(QueryParams params) throws PersonStorageException;

    void delete(String name) throws PersonStorageException;

    @ExceptionTranslation(source = ConditionalCheckFailedException.class, destination = PersonAlreadyExistsException.class)
    Person create(Person partial) throws PersonAlreadyExistsException, PersonStorageException;

    @ExceptionTranslation(source = ConditionalCheckFailedException.class, destination = PersonNotFoundException.class)
    Person update(Person partial) throws PersonNotFoundException, PersonStorageException;

    Person put(Person completePerson) throws PersonStorageException;
}
```

Supplying the `keys` field on a `@Repository` is totally optional, but helps the code generator create
primary keys for whatever implemenation its generating. The resulting class looks something like this:

``` java
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.attribute_exists;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.attribute_not_exists;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.SS;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.N;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.NS;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.L;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.M;
import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.BOOL;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.xspec.Condition;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.SetAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryResult;
import me.philcali.db.dynamo.IRetrievalStrategy;
import me.philcali.db.dynamo.QueryRetrievalStrategy;

public class PersonRepositoryDynamo implements PersonRepository {
    private final String tableName;
    private final DynamoDB db;
    private final ObjectMapper mapper;
    private final IRetrievalStrategy query;

    public PersonRepositoryDynamo(final String tableName, final DynamoDB db, final ObjectMapper mapper) {
        this.tableName = tableName;
        this.db = db;
        this.mapper = mapper;
        this.query = QueryRetrievalStrategy.fromTable(db.getTable(tableName));
    }

    public PersonRepositoryDynamo(final String tableName, final DynamoDB db) {
        this(tableName, db, new ObjectMapper());
    }

    protected <T> T fromJson(final String json, final Class<T> targetClass) {
        try {
            return mapper.readValue(json, targetClass);
        } catch (IOException ie) {
            throw new RuntimeException("Failed to parse into " + targetClass.getSimpleName(), ie);
        }
    }

    protected <T> T fromItem(final Item item, final Class<T> targetClass) {
        return fromJson(item.toJSON(), targetClass);
    }

    protected String toJson(final Object thing) {
        try {
            return mapper.writeValueAsString(thing);
        } catch (IOException ie) {
            throw new RuntimeException("Failed to serialize " + thing.getClass().getSimpleName(), ie);
        }
    }

    protected Item toItem(final Object thing) {
        return Item.fromJSON(toJson(thing));
    }

    protected JsonNode toJsonNode(final Object thing) {
        try {
            return mapper.readTree(toJson(thing));
        } catch (IOException ie) {
            throw new RuntimeException("Failed to restore " + thing.getClass().getSimpleName(), ie);
        }
    }


    @Override
    public Optional<me.philcali.zero.lombok.example.Person> get(

        final java.lang.String name) {
        try {
            final Item item = db.getTable(tableName).getItem(new PrimaryKey()
                    .addComponent("name", name));
            return Optional.ofNullable(item).map(i -> fromItem(i, me.philcali.zero.lombok.example.Person.class));

        } catch (com.amazonaws.SdkBaseException e) {
            throw new me.philcali.db.processor.example.exception.PersonStorageException(String.format("Failed to %s entity %s", "get", me.philcali.zero.lombok.example.Person.class), e);

        }
    }



    @Override
    public QueryResult<me.philcali.zero.lombok.example.Person> list(final QueryParams params) {
        final Function<Item, me.philcali.zero.lombok.example.Person> thunk = item -> fromItem(item, me.philcali.zero.lombok.example.Person.class);
        try {
            return query.andThen(result -> result.map(thunk)).apply(params, db.getTable(tableName));

        } catch (com.amazonaws.SdkBaseException e) {
            throw new me.philcali.db.processor.example.exception.PersonStorageException(String.format("Failed to %s entity %s", "list", me.philcali.zero.lombok.example.Person.class), e);

        }
    }


    /**
     * Internal helper method to create a dynamo spec that ensures existing entities
     * are not overwritten. The key values are pulled from a the Repository annotation
     * value or from the me.philcali.zero.lombok.example.Person if a method is annotated.
     */
    protected ExpressionSpecBuilder prepareCreateSpec(final Item partial) {
        final ExpressionSpecBuilder builder = new ExpressionSpecBuilder();
        builder.withCondition(
                attribute_not_exists("name"));
        return builder;
    }

    protected Item prepareItemForCreate(final me.philcali.zero.lombok.example.Person partial) {
        return toItem(partial);
    }

    /**
     * Internal helper method to prepare the put item spec for create calls. This method
     * prepares an ExpressionSpecBuilder targeted at creating new entities.
     */
    protected PutItemSpec prepareCreate(final Item partial) {
        return new PutItemSpec()
                .withItem(partial)
                .withExpressionSpec(prepareCreateSpec(partial).buildForPut());
    }

    @Override
    public me.philcali.zero.lombok.example.Person create(final me.philcali.zero.lombok.example.Person partial) {
        try {
            final Item createItem = prepareItemForCreate(partial);
            db.getTable(tableName).putItem(prepareCreate(createItem));
            return fromItem(createItem, me.philcali.zero.lombok.example.Person.class);

        } catch (com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException e) {
            throw new me.philcali.db.processor.example.exception.PersonAlreadyExistsException(String.format("Failed to %s entity %s", "create", partial), e);

        } catch (com.amazonaws.SdkBaseException e) {
            throw new me.philcali.db.processor.example.exception.PersonStorageException(String.format("Failed to %s entity %s", "create", partial), e);

        }
    }


    protected void applyUpdate(final String key, final JsonNode value, final ExpressionSpecBuilder builder) {
        if (value.isTextual()) {
            builder.addUpdate(S(key).set(value.asText()));
        } else if (value.isBoolean()) {
            builder.addUpdate(BOOL(key).set(value.asBoolean()));
        } else if (value.isNumber()) {
            builder.addUpdate(N(key).set(value.numberValue()));
        } else if (value.isArray()) {
            builder.addUpdate(L(key).set(fromJson(toJson(value), ArrayList.class)));
        } else if (value.isObject() || value.isPojo()) {
            builder.addUpdate(M(key).set(fromJson(toJson(value), HashMap.class)));
        }
    }

    protected ExpressionSpecBuilder prepareUpdateSpec(final JsonNode partial) {
        final ExpressionSpecBuilder builder = new ExpressionSpecBuilder();
        final List<String> keyNames = Arrays.asList("name");
        partial.fields().forEachRemaining(entry -> {
            if (!keyNames.stream().anyMatch(entry.getKey()::equals)) {
                applyUpdate(entry.getKey(), entry.getValue(), builder);
            }
        });
        keyNames.stream()
                .map(ExpressionSpecBuilder::attribute_exists)
                .map(condition -> (Condition) condition)
                .reduce((left, right) -> left.and(right))
                .ifPresent(builder::withCondition);
        return builder;
    }

    protected UpdateItemSpec prepareUpdate(final me.philcali.zero.lombok.example.Person partial) {
        final JsonNode partialUpdate = toJsonNode(partial);
        return new UpdateItemSpec()
                .withPrimaryKey(new PrimaryKey()
                        .addComponent("name", partialUpdate.get("name").asText()))
                .withExpressionSpec(prepareUpdateSpec(partialUpdate).buildForUpdate())
                .withReturnValues(ReturnValue.ALL_NEW);
    }

    @Override
    public me.philcali.zero.lombok.example.Person update(final me.philcali.zero.lombok.example.Person partial) {
        final ExpressionSpecBuilder builder = new ExpressionSpecBuilder();
        try {
            final Item updatedItem = db.getTable(tableName).updateItem(prepareUpdate(partial)).getItem();
            return fromItem(updatedItem, me.philcali.zero.lombok.example.Person.class);

        } catch (com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException e) {
            throw new me.philcali.db.processor.example.exception.PersonNotFoundException(String.format("Failed to %s entity %s", "update", partial), e);

        } catch (com.amazonaws.SdkBaseException e) {
            throw new me.philcali.db.processor.example.exception.PersonStorageException(String.format("Failed to %s entity %s", "update", partial), e);

        }
    }


    /**
     * Prepares a PutItemSpec that replaces the current value
     */
    protected PutItemSpec preparePut(final me.philcali.zero.lombok.example.Person replacement) {
        return new PutItemSpec()
                .withItem(toItem(replacement))
                .withReturnValues(ReturnValue.ALL_NEW);
    }

    @Override
    public me.philcali.zero.lombok.example.Person put(final me.philcali.zero.lombok.example.Person replacement) {
        try {
            final Item replacedItem = db.getTable(tableName).putItem(preparePut(replacement)).getItem();
            return fromItem(replacedItem, me.philcali.zero.lombok.example.Person.class);

        } catch (com.amazonaws.SdkBaseException e) {
            throw new me.philcali.db.processor.example.exception.PersonStorageException(String.format("Failed to %s entity %s", "put", replacement), e);

        }
    }


    protected DeleteItemSpec prepareDelete(

        final java.lang.String name) {
        return new DeleteItemSpec()
                .withPrimaryKey(new PrimaryKey()
                        .addComponent("name", name));
    }

    @Override
    public void delete(

        final java.lang.String name) {
        try {
            db.getTable(tableName).deleteItem(prepareDelete(
                    name));

        } catch (com.amazonaws.SdkBaseException e) {
            throw new me.philcali.db.processor.example.exception.PersonStorageException(String.format("Failed to %s entity %s", "delete", ""), e);

        }
    }

}
```

## What is a repository?

In the terms of code generation, it's listerally:

- __Create__: ensures that a conflict will arise if a key exists
- __Update__: ensures that an update will occur on an existing key
- __Delete__: removes an existing key
- __Read__: Optionally retrieves an entity
- __List__: Retrives a list of entities
- __Put__: Fully replaces the entity with no exception

## What if my repository is specialized?

In the case the code generator cannot complete the interface, it'll generate
an abstract repository for you, with the name `Abstract{simpleName}Dynamo`.

With this approach you can explicitly implement the methods that you are
specializing.

