package me.philcali.db.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import me.philcali.db.api.QueryParams.Collation;

public class QueryParamsTest {
    private QueryParams params;

    @Before
    public void setUp() {
        params = QueryParams.builder()
                .withCollation(Collation.DESCENDING)
                .withMaxSize(200)
                .withConditions(Conditions.attribute("name").equalsTo("Philip"))
                .build();
    }

    @Test
    public void testParams() {
        assertEquals(200, params.getMaxSize());
        assertEquals(Collation.DESCENDING, params.getCollation());
        assertTrue(params.getConditions().containsKey("name"));
        assertEquals(ICondition.Comparator.EQUALS, params.getConditions().get("name").getComparator());
        assertEquals("Philip", params.getConditions().get("name").getValue());
        assertNull(params.getToken());
    }

}
