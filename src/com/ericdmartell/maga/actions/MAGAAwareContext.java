package com.ericdmartell.maga.actions;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;

import javax.sql.DataSource;

public abstract class MAGAAwareContext {

    private MAGA             maga;

    protected MAGA getMAGA()  {
        return maga;
    }

    protected DataSource getDataSourceRead() {
        return maga.dataSourceRead;
    }

    protected DataSource getDataSourceWrite() {
        return maga.dataSourceRead;
    }

    protected MAGALoadTemplate getLoadTemplate() {
        return maga.loadTemplate;
    }

    protected MAGACache getCache() {
        return maga.cache;
    }

    public MAGAAwareContext(MAGA maga) {
        this.maga = maga;
    }

}
