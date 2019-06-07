package com.ericdmartell.maga.cache;

import java.util.List;

public class EventCountListener implements CacheEventListener {

    public long singleHits   = 0;
    public long singleMisses = 0;
    public long singleSets   = 0;

    public long bulkHits   = 0;
    public long bulkMisses = 0;
    public long bulkSets   = 0;
    public long bulkTrips  = 0;

    public long dirties = 0;
    public long flushes = 0;

    @Override
    public void onSingleHit(String key) {
        singleHits++;
    }

    @Override
    public void onSingleMiss(String key) {
        singleMisses++;
    }

    @Override
    public void onSingleSet(String key) {
        singleSets++;
    }

    @Override
    public void onBulkHit(List<String> keys) {
        bulkHits += keys.size();
    }

    @Override
    public void onBulkMiss(List<String> keys) {
        bulkMisses += keys.size();
    }

    @Override
    public void onBulkSet(List<String> keys) {
        bulkSets += keys.size();
    }

    @Override
    public void onBulkTrip() {
        bulkTrips++;
    }

    @Override
    public void onDirty(String key) {
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
        return singleHits + singleSets + singleMisses + bulkTrips + dirties + flushes;
    }
}
