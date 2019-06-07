package com.ericdmartell.maga.cache;

import org.apache.commons.collections4.CollectionUtils;

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

	private void onSingleMiss(String key) {
		eventListeners.stream().forEach(cel -> cel.onSingleMiss(key));
	}

	private void onSingleHit(String key) {
		eventListeners.stream().forEach(cel -> cel.onSingleHit(key));
	}

	private void onSingleSet(String key) {
		eventListeners.stream().forEach(cel -> cel.onSingleSet(key));
	}

	private void onBulkMiss(List<String> keys) {
		eventListeners.stream().forEach(cel -> cel.onBulkMiss(keys));
	}

	private void onBulkHit(List<String> keys) {
		eventListeners.stream().forEach(cel -> cel.onBulkHit(keys));
	}

	private void onBulkTrip() {
		eventListeners.stream().forEach(CacheEventListener::onBulkTrip);
	}

	private void onDirty(String key) {
		eventListeners.stream().forEach(cel -> cel.onDirty(key));
	}

	private void onFlush() {
		eventListeners.stream().forEach(CacheEventListener::onFlush);
	}

	public Object get(String key) {
		Object ret = getImpl(key);
		if (ret != null) {
			onSingleHit(key);
		} else {
			onSingleMiss(key);
		}
		return ret;
	}
	public void set(String key, Object val) {
		onSingleSet(key);
		setImpl(key, val);
	}
	public void flush() {
		onFlush();
		flushImpl();
	}
	public void dirty(String key) {
		onDirty(key);
		dirtyImpl(key);
	}
	public Map<String, Object> getBulk(List<String> keys) {
		Map<String, Object> ret = getBulkImpl(keys);
		onBulkHit(new ArrayList<>(ret.keySet()));
		onBulkMiss(new ArrayList<>(CollectionUtils.subtract(keys, ret.keySet())));
		onBulkTrip();
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
