package com.ericdmartell.maga.cache;

import com.ericdmartell.maga.actions.ObjectDelete;

import java.util.List;

public interface CacheEventListener {

    public void onSingleHit();
    public void onSingleMiss();
    public void onSingleSet();
    public void onSingleTrip();

    public void onBulkHit(int cnt);
    public void onBulkMiss(int cnt);
    public void onBulkSet(int cnt);
    public void onBulkTrip();

    public void onDirty();
    public void onFlush();
}
