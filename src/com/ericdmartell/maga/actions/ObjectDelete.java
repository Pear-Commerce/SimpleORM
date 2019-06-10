package com.ericdmartell.maga.actions;

import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;

public class ObjectDelete extends MAGAAwareContext {

	@Deprecated
	public ObjectDelete(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
		super(maga);
	}

	public ObjectDelete(MAGA maga) {
		super(maga);
	}

	public void delete(MAGAObject obj) {
		//Delete assocs and object itself.
		List assocs = getMAGA().loadWhereHasClass(MAGAAssociation.class);
		for (Object assoc : assocs) {
			getMAGA().deleteAssociations(obj, (MAGAAssociation) assoc);
		}
		JDBCUtil.executeUpdate("delete from `" + obj.getClass().getSimpleName() + "` where id = ?", getDataSourceWrite(), obj.id);
		MAGACache cache = getCache();
		if (cache != null) {
			cache.dirtyObject(obj);
			dirtyLoadAll(obj.getClass());

		}
	}

	private <T extends MAGAObject> void dirtyLoadAll(Class<T> clazz) {
		MAGACache cache = getCache();
		if (cache != null) {
			cache.dirty("LoadAll:" + clazz.getSimpleName());
		}
	}
	
	
}
