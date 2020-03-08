package com.ericdmartell.maga.actions;

import com.ericdmartell.maga.cache.Cache;
import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.IndexCacheKey;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.ReflectionUtils;

import javax.sql.DataSource;
import java.util.List;

public class IndexLoad extends MAGAAwareContext {

    @Deprecated
    public IndexLoad(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
        super(maga);
    }

    public  IndexLoad(MAGA maga) {
        super(maga);
    }

    public <T extends MAGAObject> List<Long> load(Class<T> clazz, String columnName, Object value) {
        List<Long>          ids = null;

        Cache cache = getCache();
        if (!ReflectionUtils.getCacheIndexedColumns(clazz).contains(columnName)) {
            throw new MAGAException("Field %s is not annotated with MAGAORMField(isCacheIndex = true).  This means indexes will not be properly dirtied when that field changes.");
        }
        final IndexCacheKey cacheKey = IndexCacheKey.getIndex(clazz, columnName, value);
        if (cache != null) {
            ids = (List<Long>) cache.get(cacheKey.toString());
        }
        if (ids == null) {
            ids = getMAGA().buildObjectLoad().loadIdsWhereExtra(clazz, String.format("`%s`=?", columnName), null, value);

            if (cache != null) {
                cache.set(cacheKey.toString(), ids);
            }
        }

        return ids;
    }

    public <T extends MAGAObject> void dirty(Class<T> aClass, String columnName, Object value) {
        final IndexCacheKey cacheKey = IndexCacheKey.getIndex(aClass, columnName, value);
        Cache cache = getCache();
        if (cache != null) {
            cache.dirty(cacheKey.toString());
        }
    }

}
