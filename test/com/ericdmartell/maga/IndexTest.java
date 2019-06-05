package com.ericdmartell.maga;

import com.ericdmartell.maga.annotations.MAGAORMField;
import com.ericdmartell.maga.cache.CacheEventListener;
import com.ericdmartell.maga.cache.EventCountListener;
import com.ericdmartell.maga.objects.MAGAObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class IndexTest extends BaseMAGATest {

    public static class IndexTestObject extends MAGAObject<IndexTestObject> {

        @MAGAORMField(isIndex = true)
        String name;

        @MAGAORMField(isIndex = false)
        String gender;
    }

    IndexTestObject testObject;

    @Before
    public void init() {
        testObject = new IndexTestObject();
        testObject.name = "Alex-" + System.currentTimeMillis();
        testObject.gender = "male";
        MAGA orm = getMAGA();
        orm.schemaSync();
        orm.buildObjectUpdate().addSQL(testObject);
    }

    @Test
    public void testCaching() {
        MAGA orm = getMAGA();
        orm.cache.resetStats();
        orm.cache.flush();

        EventCountListener counts = orm.cache.getCountListener();
        int                hits   = 0;
        int                misses = 0;
        int                sets   = 0;

        //
        // Verify cache behavior for single indexed object
        //

        List<IndexTestObject> objects = orm.loadByIndex(IndexTestObject.class, "name", testObject.name);
        Assert.assertEquals(hits = hits + 0, counts.keyHits());
        // 2 misses: 1 for the index, one for the object
        Assert.assertEquals(misses = misses + 2, counts.keyMisses());
        // 2 sets: 1 for the index, one for the object
        Assert.assertEquals(sets = sets + 2, counts.keySets());
        Assert.assertEquals(1, objects.size());

        objects = orm.loadByIndex(IndexTestObject.class, "name", testObject.name);
        // 2 hits: 1 for the index, one for the object
        Assert.assertEquals(hits = hits + 2, counts.keyHits());
        Assert.assertEquals(misses = misses + 0, counts.keyMisses());
        Assert.assertEquals(sets = sets + 0, counts.keySets());
        Assert.assertEquals(1, objects.size());

        //
        // Change indexed value and verify cache behavior
        //

        IndexTestObject object = objects.get(0);
        String oldName = object.name;
        object.name = "Eric-" + System.currentTimeMillis();
        orm.buildObjectUpdate().update(object);
        // Note: updating causes an additional load/hit on the object
        Assert.assertEquals(hits = hits + 1, counts.keyHits());
        // Note: updating also causes a miss when trying to load template_dependencies
        Assert.assertEquals(misses = misses + 1, counts.keyMisses());
        // 1 new set since MAGA is set to writeThroughCacheOnUpdate
        Assert.assertEquals(sets = sets + 1, counts.keySets());

        objects = orm.loadByIndex(IndexTestObject.class, "name", oldName);
        // no new hits because the name has changed!
        Assert.assertEquals(hits = hits + 0, counts.keyHits());
        // 1 new miss, because the index does not exist so the object does not get loaded
        Assert.assertEquals(misses = misses + 1, counts.keyMisses());
        // 1 new set, because the index does not exist so the object does not get loaded
        Assert.assertEquals(sets = sets + 1, counts.keySets());
        Assert.assertEquals(0, objects.size());

        objects = orm.loadByIndex(IndexTestObject.class, "name", object.name);
        // 1 new hits, the object was set-on-write
        Assert.assertEquals(hits = hits + 1, counts.keyHits());
        // 1 new miss for the index
        Assert.assertEquals(misses = misses + 1, counts.keyMisses());
        // 1 new set for the index
        Assert.assertEquals(sets = sets + 1, counts.keySets());
        Assert.assertEquals(1, objects.size());

        objects = orm.loadByIndex(IndexTestObject.class, "name", object.name);
        Assert.assertEquals(hits = hits + 2, counts.keyHits());
        Assert.assertEquals(misses = misses + 0, counts.keyMisses());
        Assert.assertEquals(sets = sets + 0, counts.keySets());
        Assert.assertEquals(1, objects.size());

        //
        // Create another matching indexed object and verify cache behavior
        //

        IndexTestObject femaleVersion = new IndexTestObject();
        femaleVersion.name = object.name;
        femaleVersion.gender = "female";
        orm.buildObjectUpdate().addSQL(femaleVersion);
        // Sanity check changes during creation of new object
        Assert.assertEquals(hits = hits + 0, counts.keyHits());
        Assert.assertEquals(misses = misses + 0, counts.keyMisses());
        // one new set because MAGA is set to writeThroughCacheOnUpdate = true
        Assert.assertEquals(sets = sets + 1, counts.keySets());

        objects = orm.loadByIndex(IndexTestObject.class, "name", femaleVersion.name);
        // 2 new hits: 1 for the male object, 1 for the female object
        Assert.assertEquals(hits = hits + 2, counts.keyHits());
        // 1 new miss for the index
        Assert.assertEquals(misses = misses + 1, counts.keyMisses());
        // 1 new sets for the index
        Assert.assertEquals(sets = sets + 1, counts.keySets());
        Assert.assertEquals(2, objects.size());

    }
}
