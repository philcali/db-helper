package me.philcali.db.api;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QueryResult<T> {
    private final Optional<IPageKey> token;
    private final List<T> items;
    private final boolean truncated;

    public QueryResult(final Optional<IPageKey> token, final List<T> items, final boolean truncated) {
        this.token = token;
        this.items = items;
        this.truncated = truncated;
    }

    public List<T> getItems() {
        return items;
    }

    public Optional<IPageKey> getToken() {
        return token;
    }

    public boolean isTruncated() {
        return truncated;
    }

    // I kind of hate this .. but moving along
    public <V> QueryResult<V> map(final Function<T, V> thunk) {
        return new QueryResult<>(token, items.stream().map(thunk).collect(Collectors.toList()), truncated);
    }
}
