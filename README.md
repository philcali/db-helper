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
