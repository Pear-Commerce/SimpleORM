package com.ericdmartell.maga.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import org.apache.commons.lang.StringUtils;

public class MemcachedCache extends MAGACache {
	
	private MemcachedClient memcachedClient;

	public MemcachedCache(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
	}
	
	public Object getImpl(String key) {
		return memcachedClient.get(sanitizeKey(key));
	}
	
	public void setImpl(String key, Object val) {
		OperationFuture<Boolean> future = memcachedClient.set(sanitizeKey(key), Integer.MAX_VALUE, val);
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
		OperationFuture<Boolean> future = memcachedClient.delete(sanitizeKey(key));
		try {
			future.get(2, TimeUnit.SECONDS);
		} catch (InterruptedException | TimeoutException  | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Object> getBulkImpl(List<String> keys) {
		Map<String, String> sanitizedKeys = keys.stream().collect(
				Collectors.toMap(MemcachedCache::sanitizeKey, Function.identity()));
		Map<String, Object> sanitizedResults = memcachedClient.getBulk(sanitizedKeys.keySet());
		return sanitizedResults.entrySet().stream().collect(
				Collectors.toMap(sanitizedEntry -> sanitizedKeys.get(sanitizedEntry.getKey()), Map.Entry::getValue));
	}

	public static String sanitizeKey(String key) {
		return StringUtils.abbreviate(key.replaceAll("[\\s\\n\\r\0]", "_"), 250);
	}
}
