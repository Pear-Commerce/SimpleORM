package com.ericdmartell.maga.actions;

import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;

public class ObjectDelete extends MAGAAction {

	public ObjectDelete(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
		super(dataSource, cache, maga, template);
	}

	public void delete(MAGAObject obj) {
		//Delete assocs and object itself.
		List assocs = maga.loadWhereHasClass(MAGAAssociation.class);
		for (Object assoc : assocs) {
			maga.deleteAssociations(obj, (MAGAAssociation) assoc);
		}
		JDBCUtil.executeUpdate("delete from `" + obj.getClass().getSimpleName() + "` where id = ?", dataSource, obj.id);
		cache.dirtyObject(obj);
	}
	
	
}
