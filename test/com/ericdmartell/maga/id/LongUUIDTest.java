package com.ericdmartell.maga.id;

import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alexwyler on 9/4/18.
 */
public class LongUUIDTest {

    @Test
    public void testBits() {
        long        beforeTimestamp = System.currentTimeMillis();
        LongUUIDGen gen             = new LongUUIDGen(2, 3);

        long uuid = gen.getNext();
        Assert.assertEquals(3, gen.getSource(uuid));
        long timestamp = gen.getJavaTimestamp(uuid);
        Assert.assertTrue(beforeTimestamp <= timestamp);
        long afterTimestamp = System.currentTimeMillis();
        Assert.assertTrue(timestamp <= afterTimestamp);

        // Check for Javascript compatibility
        Assert.assertTrue(uuid < (Math.pow(2, 53) - 1));
    }

    @Test
    public void testOffset() {
        boolean     reusedOffset = false;
        Integer     oldOffset    = null;
        long        oldUUID      = 0;
        LongUUIDGen gen          = new LongUUIDGen(13, 7);

        DateFormat fmt = new SimpleDateFormat("mm:ss:SS");
        long[] genUuids = gen.getNext(1000);
        for (int i = 0; i < genUuids.length; i++) {
            long uuid = genUuids[i];
            int offset = gen.getSourceOffset(uuid);
            int source = gen.getSource(uuid);
            System.out.println(fmt.format(gen.getCreationDate(uuid)) + " " + (uuid & 0x1FFF) + " " + source + " " + offset);
            if (oldOffset != null) {
                if (offset == oldOffset) {
                    reusedOffset = true;
                }
            }
            Assert.assertNotEquals(oldUUID, uuid);
            oldUUID = uuid;
            oldOffset = offset;
        }

        // make sure we are incrementing uuids
        Assert.assertTrue(reusedOffset);
    }
}


