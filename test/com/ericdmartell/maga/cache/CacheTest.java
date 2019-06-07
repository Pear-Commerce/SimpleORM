package com.ericdmartell.maga.cache;

import com.ericdmartell.maga.BaseMAGATest;
import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.Obj1;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheTest extends BaseMAGATest {

    @Test
    public void testNoCache() {
        MAGA noCacheOrm = new MAGA().withDataSource(getDataSource());
        noCacheOrm.schemaSync();
        Obj1 obj1 = new Obj1();
        obj1.field1 = "This is a test of field one";
        noCacheOrm.save(obj1);

        Obj1 obj1b = noCacheOrm.load(Obj1.class, obj1.id);
        Assert.assertNotNull(obj1b);

        Obj1 obj1c = noCacheOrm.loadByIndexSingle(Obj1.class, "field1", "This is a test of field one");
        Assert.assertNotNull(obj1c);

        noCacheOrm.delete(obj1);
    }

    @Test
    public void testEventListener() {
        final AtomicInteger countEvents = new AtomicInteger(0);
        HashMapCache instrumentedCache = new HashMapCache(1000);
        instrumentedCache.withEventListener(new CacheEventListener() {
            @Override
            public void onSingleHit() {

            }

            @Override
            public void onSingleMiss() {

            }

            @Override
            public void onSingleSet() {
                countEvents.incrementAndGet();
            }

            @Override
            public void onBulkHit(int cnt) {

            }

            @Override
            public void onBulkMiss(int cnt) {

            }

            @Override
            public void onBulkSet(int cnt) {

            }

            @Override
            public void onBulkTrip() {

            }

            @Override
            public void onDirty() {

            }

            @Override
            public void onFlush() {

            }
        });

        MAGA instrumentedCacheOrm = new MAGA().withDataSource(getDataSource()).withCache(instrumentedCache);

        Obj1 obj1 = new Obj1();
        obj1.field1 = "Test";
        instrumentedCacheOrm.save(obj1);

        Assert.assertEquals(1, countEvents.get());

        Obj1 obj2 = new Obj1();
        obj1.field1 = "Test2";
        instrumentedCacheOrm.save(obj2);

        Assert.assertEquals(2, countEvents.get());

    }
}
