package com.ericdmartell.maga.utils;

import com.ericdmartell.maga.objects.MAGAObject;

import java.util.Arrays;

import org.apache.commons.lang.ObjectUtils;

public class IndexCacheKey {
    public static final ThreadLocal<IndexCacheKey> tl = ThreadLocal.withInitial(() -> new IndexCacheKey());

    public String[] key = new String[3];
    public int hash = 0;
    public Class<?> type;
    public String name;
    public Object value;
    public boolean dirty;

    public static IndexCacheKey getIndex(Class<? extends MAGAObject> type, String name, Object value) {
        final IndexCacheKey objectKey = tl.get();
        objectKey.setAsIndex(type, name, value);

        return objectKey;
    }

    public void setAsIndex(Class<? extends MAGAObject> type, String name, Object value) {
        this.type = type;
        this.name = name;
        this.value = value;
        dirty = false;
        final String[] key = this.key;
        key[0] = type.getName();
        key[1] = name;
        key[2] = String.valueOf(value);
        hash = Arrays.hashCode(key);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IndexCacheKey) {
            final IndexCacheKey objectKey = (IndexCacheKey) obj;
            if (dirty || objectKey.dirty) {
                return HashUtil.arrayValueEquals(key, objectKey.key);
            } else {
                return ObjectUtils.equals(objectKey.type, type)
                        && ObjectUtils.equals(objectKey.name, name)
                        && ObjectUtils.equals(objectKey.value, value);
            }
        }

        return false;
    }

    public IndexCacheKey copy() {
        final IndexCacheKey indexKey = new IndexCacheKey();
        indexKey.hash = hash;
        indexKey.value = value;
        indexKey.name = name;
        indexKey.type = type;
        indexKey.dirty = this.dirty;
        final String[] key = indexKey.key;
        final String[] thisKey = this.key;
        System.arraycopy(thisKey, 0, key, 0, thisKey.length);

        return indexKey;
    }

    @Override
    public String toString() {
        return ("INDEX," + key[0] + ',' + key[1] + ',' + key[2]);
    }

}
