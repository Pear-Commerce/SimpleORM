package com.ericdmartell.maga.id;


import java.util.Date;

/**
 * Considerations for a LongUUIDGen Generator:
 *
 * 1. For use as a numeric in browser javascript, UUIDs should be no larger than 53 bit longs (http://stackoverflow.com/questions/307179/what-is-javascripts-max-int-whats-the-highest-integer-value-a-number-can-go-t)
 * 2. Each source should be able to generate it's own UUIDs without relying a central point of failure.
 * 3. UUIDs should be in chronological order (ie: x < y means x generated before y), and it should be possile to retrieve the actual creation timestamp for the UUID.
 * 4. We should never bottleneck on UUID creation, so it should be require no web calls and should have high throughput per second.
 *
 * The approach in this file satisfies all of the above constraints.
 * Assuming all of 2^13 sources are in use, a max throughput of 1000 new uuids per second per source can be achieved. (8+ million new uuids per second) overall.
 * When the actual source count is lower, we can purposely use known unused server ids to multiply the throughput per server to also get closer to the theoretical maximum throughput.
 * With the configuration of a maximum of 32 actual sources, each can generate over 200k uuids per second.
 *
 * Layout:
 * [-- unused, 11 bits -- ][ -- timestamp in ms (offset from 9/22/14 for readability), 40 bits -- ][ -- source id and offset, 13 bits -- ]
 */
public class LongUUIDGen extends IDGen {

    private static long TIMESTAMP_OFFSET_FROM = 1411424944543l;

    private static int THEORETICAL_MAX_BITS_FOR_SOURCE = 13;

    private long lastTimestamp = 0;

    private int    sourceId;

    /**
     * To maximize throughput on low number of sources, we can allow servers to use the lowest bits for offsets into
     * the current millisecond.
     */
    private int sourceOffset = 0;
    private int bitsForSource;

    public static long getUuidFromTimestamp(long timestamp) {
        return (timestamp - TIMESTAMP_OFFSET_FROM) << THEORETICAL_MAX_BITS_FOR_SOURCE;
    }

    public LongUUIDGen(int bitsForSource, int sourceId) {
        if (bitsForSource > THEORETICAL_MAX_BITS_FOR_SOURCE || bitsForSource < 1) {
            throw new RuntimeException("bitsForSource must be between 1 and " + THEORETICAL_MAX_BITS_FOR_SOURCE);
        }
        this.bitsForSource = bitsForSource;
        this.sourceId = sourceId;
    }

    public LongUUIDGen(int sourceId) {
        this.bitsForSource = 8;
        this.sourceId = sourceId;
    }

    public long[] getNext(int number) {
        long[] generatedIds = new long[number];
        for (int i = 0; i < number; i++) {
            generatedIds[i] = getNext();
        }
        return generatedIds;
    }

    @Override
    public synchronized long getNext() {
        long timestamp = System.currentTimeMillis();
        int bitsForOffset = (THEORETICAL_MAX_BITS_FOR_SOURCE - bitsForSource);
        long sourcePlusOffset = this.sourceId << bitsForOffset;
        if (timestamp == lastTimestamp) {
            sourceOffset++;
            if (1 << bitsForOffset >= sourceOffset) {
                try {
                    // sleep for a millisecond to wait for more ids to be
                    // available
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timestamp++;
                sourceOffset = 0;
            }
        }
        sourcePlusOffset += sourceOffset;
        lastTimestamp = timestamp;

        return (timestamp - TIMESTAMP_OFFSET_FROM) << THEORETICAL_MAX_BITS_FOR_SOURCE | sourcePlusOffset & 0x1FFF;
    }

    public int getSource(long uuid) {
        return (int) ((uuid & 0x1FFF) >> (THEORETICAL_MAX_BITS_FOR_SOURCE - bitsForSource));
    }

    public int getSourceOffset(long uuid) {
        return (int) (uuid & ((1 << (THEORETICAL_MAX_BITS_FOR_SOURCE - bitsForSource)) - 1));
    }

    public long getTimestampMillisRaw(long uuid) {
        return uuid >>> THEORETICAL_MAX_BITS_FOR_SOURCE;
    }

    public long getJavaTimestamp(long uuid) {
        return getTimestampMillisRaw(uuid) + TIMESTAMP_OFFSET_FROM;
    }

    public long getUnixTimestamp(long uuid) {
        return getJavaTimestamp(uuid) / 1000;
    }

    public Date getCreationDate(long uuid) {
        return new Date(getJavaTimestamp(uuid));
    }
}
