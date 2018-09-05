package com.ericdmartell.maga.id;

import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;

/**
 * Created by alexwyler on 9/4/18.
 */
public class RandomUUIDTest {

    @Test
    public void testRandom() {
        RandomIDGen gen             = new RandomIDGen();

        HashSet<Long> set = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            long uuid = gen.getNext();
            Assert.assertTrue(set.add(uuid));
            // Check for Javascript compatibility
            Assert.assertEquals(0, uuid >> (64-53));
        }
    }

}


