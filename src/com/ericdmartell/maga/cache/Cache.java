package com.ericdmartell.maga.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Cache {

	List<CacheEventListener> eventListeners = new ArrayList<>();

	private EventCountListener countListener = new EventCountListener();
	{
		withEventListener(countListener);
	}
	public EventCountListener getCountListener() {
		return countListener;
	}

	public Cache withEventListener(CacheEventListener cel) {
		eventListeners.add(cel);
		return this;
	}

	private void onSingleMiss() {
		eventListeners.stream().forEach(CacheEventListener::onSingleMiss);
	}

	private void onSingleHit() {
		eventListeners.stream().forEach(CacheEventListener::onSingleHit);
	}

	private void onSingleSet() {
		eventListeners.stream().forEach(CacheEventListener::onSingleSet);
	}

	private void onBulkMiss(int cnt) {
		eventListeners.stream().forEach(CacheEventListener::onBulkTrip);
		eventListeners.stream().forEach(cel -> cel.onBulkMiss(cnt));
	}

	private void onBulkHit(int cnt) {
		eventListeners.stream().forEach(CacheEventListener::onBulkTrip);
		eventListeners.stream().forEach(cel -> cel.onBulkHit(cnt));
	}

	private void onBulkSet(int cnt) {
		eventListeners.stream().forEach(CacheEventListener::onBulkTrip);
		eventListeners.stream().forEach(cel -> cel.onBulkSet(cnt));
	}

	private void onDirty() {
		eventListeners.stream().forEach(CacheEventListener::onDirty);
	}

	private void onFlush() {
		eventListeners.stream().forEach(CacheEventListener::onFlush);
	}

	public Object get(String key) {
		Object ret = getImpl(key);
		if (ret != null) {
			onSingleHit();
		} else {
			onSingleMiss();
		}
		return ret;
	}
	public void set(String key, Object val) {
		onSingleSet();
		setImpl(key, val);
	}
	public void flush() {
		onFlush();
		flushImpl();
	}
	public void dirty(String key) {
		onDirty();
		dirtyImpl(key);
	}
	public Map<String, Object> getBulk(List<String> keys) {
		Map<String, Object> ret = getBulkImpl(keys);
		onBulkHit(ret.size());
		onBulkMiss(keys.size() - ret.size());
		return ret;
	}
	public void resetStats() {
		countListener.resetStats();
	}
	public abstract Object getImpl(String key);
	public abstract void setImpl(String key, Object val);
	public abstract void flushImpl();
	public abstract void dirtyImpl(String key);
	public abstract Map<String, Object> getBulkImpl(List<String> keys);
}
