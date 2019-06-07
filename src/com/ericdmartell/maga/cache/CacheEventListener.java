package com.ericdmartell.maga.cache;

import com.ericdmartell.maga.actions.ObjectDelete;

import java.util.List;

public interface CacheEventListener {

    public void onSingleHit(String key);
    public void onSingleMiss(String key);
    public void onSingleSet(String key);

    public void onBulkHit(List<String> keys);
    public void onBulkMiss(List<String> keys);
    public void onBulkSet(List<String> keys);
    public void onBulkTrip();

    public void onDirty(String key);
    public void onFlush();
}
