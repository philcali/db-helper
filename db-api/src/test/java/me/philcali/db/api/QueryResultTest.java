package me.philcali.db.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class QueryResultTest {
    private QueryResult<Integer> results;

    @Before
    public void setUp() {
        final List<Integer> ints = Arrays.asList(10, 20, 30, 40, 50);
        results = new QueryResult<>(Optional.empty(), ints, true);
    }

    @Test
    public void testMap() {
        assertEquals(Arrays.asList("10", "20", "30", "40", "50"), results.map(i -> i.toString()).getItems());
    }

    @Test
    public void testToken() {
        assertEquals(Optional.empty(), results.getToken());
    }

    @Test
    public void testTruncated() {
        assertTrue(results.isTruncated());
    }
}
