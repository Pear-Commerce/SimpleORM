package com.ericdmartell.maga.cache;

public class EventCountListener implements CacheEventListener {

    public long singleHits   = 0;
    public long singleMisses = 0;
    public long singleSets   = 0;
    public long singleTrips  = 0;

    public long bulkHits   = 0;
    public long bulkMisses = 0;
    public long bulkSets   = 0;
    public long bulkTrips  = 0;

    public long dirties = 0;
    public long flushes = 0;

    @Override
    public void onSingleHit() {
        singleHits++;
    }

    @Override
    public void onSingleMiss() {
        singleMisses++;
    }

    @Override
    public void onSingleSet() {
        singleSets++;
    }

    @Override
    public void onSingleTrip() {
        singleTrips++;
    }

    @Override
    public void onBulkHit(int cnt) {
        bulkHits += cnt;
    }

    @Override
    public void onBulkMiss(int cnt) {
        bulkMisses += cnt;
    }

    @Override
    public void onBulkSet(int cnt) {
        bulkSets += cnt;
    }

    @Override
    public void onBulkTrip() {
        bulkTrips++;
    }

    @Override
    public void onDirty() {
        dirties++;
    }

    @Override
    public void onFlush() {
        flushes++;
    }

    public void resetStats() {
        singleHits = 0;
        singleMisses = 0;
        singleSets = 0;
        singleTrips = 0;

        bulkHits = 0;
        bulkMisses = 0;
        bulkSets = 0;
        bulkTrips = 0;

        dirties = 0;
        flushes = 0;
    }

    public long keyHits() {
        return singleHits + bulkHits;
    }

    public long keyMisses() {
        return singleMisses + bulkMisses;
    }

    public long keySets() {
        return singleSets + bulkSets;
    }

    public long roundTrips() {
        return singleTrips + bulkTrips + dirties + flushes;
    }
}
