package com.ericdmartell.maga.actions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.HistoryUtil;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.ReflectionUtils;

public class AssociationAdd extends MAGAAwareContext {

	@Deprecated
	public AssociationAdd(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
		super(maga);
	}

	public AssociationAdd(MAGA maga) {
		super(maga);
	}

	public void add(MAGAObject obj, MAGAObject obj2, MAGAAssociation association) {
		if (association.type() == MAGAAssociation.MANY_TO_MANY) {
			manyToMany(obj, obj2, association);
		} else {
			if (obj.getClass() == association.class1()) {
				oneToMany(obj, obj2, association);
			} else {
				oneToMany(obj2, obj, association);
			}
		}
	}

	private void manyToMany(MAGAObject obj, MAGAObject obj2, MAGAAssociation association) {
		
		String table = association.class1().getSimpleName() + "_to_"
				+ association.class2().getSimpleName();
		
		String first = "1";
		Connection connection = null;
		try {
			connection = JDBCUtil.getConnection(getDataSourceRead());
			ResultSet rst = JDBCUtil.executeQuery(connection, "select * from " + table +" where `" + obj.getClass().getSimpleName() + "` = ? and `" + obj2.getClass().getSimpleName() + "` = ?", obj.id, obj2.id);
			if (rst.next()) {
				first = "0";
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			JDBCUtil.closeConnection(connection);
		}
		
		// DB Part
		JDBCUtil.executeUpdate("insert into `" + table + "`(`" + obj.getClass().getSimpleName() + "`,`"
				+ obj2.getClass().getSimpleName() + "`, dateAssociated, firstAssoc) values(?,?, now(), ?)", getDataSourceWrite(), obj.id, obj2.id, first);

		// Cache Part
		getCache().dirtyAssoc(obj, association);
		getCache().dirtyAssoc(obj2, association);
	}

	private void oneToMany(MAGAObject objOfClass1, MAGAObject objOfClass2, MAGAAssociation association) {
		
		// We need this because if we're adding an assoc for a one-many, we might be switching the assoc of the old one.
		MAGAObject oldOneOfTheOneToMany = null;
		List<MAGAObject> listOfOldOneOfTheOneToMany = getMAGA().loadAssociatedObjects(objOfClass2, association);
		if (!listOfOldOneOfTheOneToMany.isEmpty()) {
			oldOneOfTheOneToMany = listOfOldOneOfTheOneToMany.get(0);
		}
		
		
		// For historical changes.
		MAGAObject oldObject = getMAGA().load(objOfClass2.getClass(), objOfClass2.id);

		// DB/Live object field swap Part.
		JDBCUtil.executeUpdate("update `" + objOfClass2.getClass().getSimpleName() + "` set `" + association.class2Column()
				+ "` = ? where id = ?", getDataSourceWrite(), objOfClass1.id, objOfClass2.id);
		ReflectionUtils.setFieldValue(objOfClass2, association.class2Column(), objOfClass1.id);

		// Cache Part.
		getCache().dirtyObject(objOfClass2);
		getCache().dirtyAssoc(objOfClass2, association);
		getCache().dirtyAssoc(objOfClass1, association);
		if (oldOneOfTheOneToMany != null) {
			getCache().dirtyAssoc(oldOneOfTheOneToMany, association);
		}

		// Since we changed an actual object, we record the change.
		HistoryUtil.recordHistory(oldObject, objOfClass2, getMAGA(), getDataSourceWrite());
	}
}
