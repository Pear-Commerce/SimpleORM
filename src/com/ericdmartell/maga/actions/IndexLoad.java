package com.ericdmartell.maga.actions;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.IndexCacheKey;

import javax.sql.DataSource;
import java.util.List;

public class IndexLoad extends MAGAAction {

    public IndexLoad(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
        super(dataSource, cache, maga, template);
    }

    public <T extends MAGAObject> List<Long> load(Class<T> clazz, String columnName, Object value) {
        List<Long>          ids;
        final IndexCacheKey cacheKey = IndexCacheKey.getIndex(clazz, columnName, value);

        ids = (List<Long>) cache.get(cacheKey.toString());
        if (ids == null) {
            ids = new ObjectLoad(dataSource, cache, maga, template).loadIdsWhereExtra(clazz, String.format("`%s`=?", columnName), null, value);
            cache.set(cacheKey.toString(), ids);
        }

        return ids;
    }

    public <T extends MAGAObject> void dirty(Class<T> aClass, String columnName, Object value) {
        System.out.println("Dirtying " + aClass + " " + columnName + "=" + value);
        final IndexCacheKey cacheKey = IndexCacheKey.getIndex(aClass, columnName, value);
        cache.dirty(cacheKey.toString());
    }

}
