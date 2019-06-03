package com.ericdmartell.maga;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ericdmartell.cache.Cache;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;

public class MemcachedCache extends Cache {
	
	private MemcachedClient memcachedClient;

	public MemcachedCache(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
	}
	
	public Object getImpl(String key) {
		return memcachedClient.get(key);
	}
	
	public void setImpl(String key, Object val) {
		OperationFuture<Boolean> future = memcachedClient.set(key, Integer.MAX_VALUE, val);
		try {
			future.get(2, TimeUnit.SECONDS);
		} catch (InterruptedException | TimeoutException  | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public void flushImpl() {
		OperationFuture<Boolean> future = memcachedClient.flush();
		try {
			future.get(2, TimeUnit.SECONDS);
		} catch (InterruptedException | TimeoutException  | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public void dirtyImpl(String key) {
		OperationFuture<Boolean> future = memcachedClient.delete(key);
		try {
			future.get(2, TimeUnit.SECONDS);
		} catch (InterruptedException | TimeoutException  | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Object> getBulkImpl(List<String> keys) {
		return memcachedClient.getBulk(keys);
	}
}
