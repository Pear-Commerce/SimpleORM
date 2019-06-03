package com.ericdmartell.maga.actions;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.IndexCacheKey;

import javax.sql.DataSource;
import java.util.List;

public class IndexLoad extends MAGAAwareContext {

    @Deprecated
    public IndexLoad(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
        super(maga);
    }

    public IndexLoad(MAGA maga) {
        super(maga);
    }

    public <T extends MAGAObject> List<Long> load(Class<T> clazz, String columnName, Object value) {
        List<Long>          ids;
        final IndexCacheKey cacheKey = IndexCacheKey.getIndex(clazz, columnName, value);

        ids = (List<Long>) getCache().get(cacheKey.toString());
        if (ids == null) {
            ids = new ObjectLoad(getMAGA()).loadIdsWhereExtra(clazz, String.format("`%s`=?", columnName), null, value);
            getCache().set(cacheKey.toString(), ids);
        }

        return ids;
    }

    public <T extends MAGAObject> void dirty(Class<T> aClass, String columnName, Object value) {
        final IndexCacheKey cacheKey = IndexCacheKey.getIndex(aClass, columnName, value);
        getCache().dirty(cacheKey.toString());
    }

}
