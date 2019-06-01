package com.ericdmartell.maga.actions;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;

import javax.sql.DataSource;

public abstract class MAGAAction {

    protected DataSource       dataSource;
    protected MAGALoadTemplate template;
    protected MAGACache        cache;
    protected MAGA             maga;

    protected MAGAAction(DataSource dataSource, MAGACache cache, MAGA maga,
                      MAGALoadTemplate template) {
        this.dataSource = dataSource;
        this.cache = cache;
        this.template = template;
        this.maga = maga;
    }

}
