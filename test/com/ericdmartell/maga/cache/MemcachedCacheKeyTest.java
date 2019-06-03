package com.ericdmartell.maga.cache;

import com.ericdmartell.maga.BaseMAGATest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemcachedCacheKeyTest extends BaseMAGATest {

    @Test
    public void smokeTest() {
        String key = "Hey\nMan What's up";
        String value = "nm u?";
        getMcCache().setImpl(key, value);
        List<String> keys = new ArrayList<>();
        keys.add(key);
        Map<String, Object> results = getMcCache().getBulkImpl(keys);

        Assert.assertTrue(results.keySet().contains(key));
        Assert.assertEquals(value, results.get(key));
    }

    @Test
    public void testSanitation() {
        String key1 = "\n\r \0";
        Assert.assertEquals("____", MemcachedCache.sanitizeKey(key1));

        String key2 = "This is a test!";
        Assert.assertEquals("This_is_a_test!", MemcachedCache.sanitizeKey(key2));


    }
}
