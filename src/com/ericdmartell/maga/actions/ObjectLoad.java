package com.ericdmartell.maga.actions;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.ReflectionUtils;

public class ObjectLoad extends MAGAAwareContext {

	@Deprecated
	public ObjectLoad(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
		super(maga);
	}

	public ObjectLoad(MAGA maga) {
		super(maga);
	}

	public List<MAGAObject> loadTemplate(MAGALoadTemplate template) {
		List<MAGAObject> ret = (List<MAGAObject>) getCache().get(template.getKey());
		if (ret != null) {
			return ret;
		} else {
			ret = template.run(getMAGA());
			// save our result for next fetch.
			getCache().set(template.getKey(), ret);
			return ret;
		}
	}

	public <T extends MAGAObject> List<T> loadAll(Class<T> clazz) {
		return loadWhereExtra(clazz, "1", "");
	}

	public <T extends MAGAObject> List<T> loadWhereExtra(Class<T> clazz, String where, String extra, Object... params) {
		List<T> ret = load(clazz, loadIdsWhereExtra(clazz, where, extra, params));
		return ret;
	}

	public List<Long> loadIdsWhereExtra(Class clazz, String where, String extra, Object... params) {
		Connection connection = JDBCUtil.getConnection(getDataSourceRead());
		try {
			String sql = "select id from `" + clazz.getSimpleName() + String.format("` where %s %s", where, StringUtils.defaultString(extra, ""));
			ResultSet rst = JDBCUtil.executeQuery(connection, sql, params);
			List<Long> ids = new ArrayList<>();
			while (rst.next()) {
				ids.add(rst.getLong(1));
			}
			return ids;
		} catch (SQLException e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(connection);
		}
	}

	public <T extends MAGAObject> T load(Class<T> clazz, long id) {
		//Just a wrapper on the load collection of ids.
		List<Long> ids = new ArrayList<>();
		ids.add(id);
		List<T> retList = load(clazz, ids);
		if (retList.isEmpty()) {
			return null;
		} else {
			return retList.get(0);
		}
	}

	public <T extends MAGAObject> List<T> load(Class<T> clazz, Collection<Long> ids) {

		// A running list of ids to load
		List<Long> toLoad = new ArrayList<>(ids);

		//Remove 0's
		List<String> zeroList = new ArrayList<>();
		zeroList.add("");
		zeroList.add(null);
		toLoad.removeAll(zeroList);
		
		//Don't make trips anywhere if the list is empty
		if (toLoad.isEmpty()) {
			return new ArrayList<>();
		}
		
		// Try getting them from memcached
		List<T> ret;
		MAGACache cache = getCache();
		if (cache != null) {
			ret = getCache().getObjects(clazz, toLoad);
		} else {
			ret = new ArrayList<>();
		}

		// Remove the ids we got from memcached before going to the database.
		for (MAGAObject gotFromMemcached : ret) {
			toLoad.remove(gotFromMemcached.id);
		}

		// We still have ids that aren't in memcached, fetch from the database.
		if (!toLoad.isEmpty()) {
			List<T> dbObjects = loadFromDB(clazz, toLoad);
			for (MAGAObject gotFromDB : dbObjects) {
				toLoad.remove(gotFromDB.id);
			}

			if (cache != null) {
				//We'll have them in the cache next time.
				cache.setObjects(dbObjects, getLoadTemplate());
			}

			ret.addAll(dbObjects);
		}

		for (MAGAObject object : ret) {
			object.savePristineCacheIndexValues();
		}

		// We went to memcached, we went to the db, and we still have ids left
		// over?
		if (!toLoad.isEmpty()) {
			// System.out.println("DB Misses for " + toLoad);
		}
		
		if (getLoadTemplate() != null && cache != null) {
			for (MAGAObject object : ret) {
				cache.addTemplateDependency(object, getLoadTemplate());
			}
		}
		return ret;
	}

	private <T extends MAGAObject> List<T> loadFromDB(Class<T> clazz, Collection<Long> ids) {
		List<T> ret = new ArrayList<>();

		// Fields with annotations
		List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(clazz));

		// Sql to bulk fetch all ids.
		String sql = getSQL(clazz, fieldNames, ids);
		Connection connection = JDBCUtil.getConnection(getDataSourceRead());

		try {
			ResultSet rst = JDBCUtil.executeQuery(connection, sql, ids);
			while (rst.next()) {
				T entity =  ReflectionUtils.getEntityFromResultSet(clazz, rst);
				ret.add(entity);
			}
		} catch (Exception e) {
			throw new MAGAException(e);
		} finally {
			JDBCUtil.closeConnection(connection);
		}
		return ret;

	}

	private <T extends MAGAObject> String getSQL(Class<T> clazz, Collection<String> fieldNames, Collection<Long> ids) {
		String sql = "select ";
		for (String fieldName : fieldNames) {
			sql += "`" + fieldName + "`,";
		}
		sql = sql.substring(0, sql.length() - 1);
		sql += " from `" + clazz.getSimpleName();
		if (ids.size() == 1) {
			sql += "` where id = ?";
		} else {
			sql += "` where id in (";
			for (long id : ids) {
				sql +=  "?,";
			}
			sql = sql.substring(0, sql.length() - 1);
			sql += ")";
		}
		return sql;
	}

}
