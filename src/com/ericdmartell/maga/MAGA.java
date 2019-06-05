package com.ericdmartell.maga;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.ericdmartell.cache.Cache;
import com.ericdmartell.maga.actions.*;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.HashMapCache;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.id.IDGen;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.ericdmartell.maga.utils.JSONUtil;
import com.ericdmartell.maga.utils.MAGAException;
import com.ericdmartell.maga.utils.TrackedConnection;

import com.fasterxml.jackson.databind.ObjectMapper;
import gnu.trove.set.hash.THashSet;

public class MAGA {

	public IDGen            idGen;
	public DataSource       dataSourceWrite;
	public DataSource       dataSourceRead;
	public MAGACache        cache;
	public MAGALoadTemplate loadTemplate;
	public ObjectMapper		objectMapper;
	private String			defaultCharacterSet;
	private String			defaultCollate;

	private boolean writeThroughCacheOnUpdate = true;

	public ThreadPoolExecutor executorPool = new ThreadPoolExecutor(50, 50, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(50));
	{
		executorPool.submit((Runnable) () -> {
			while (true) {
				Set<TrackedConnection> reported = null;
				for (TrackedConnection connection : JDBCUtil.openConnections) {
					if (new Date().getTime() - connection.date.getTime() >= 5000) {
						System.out.println("Leaked Connection");
						connection.stack.printStackTrace();
						// avoid creating a new hashmap every loop
						if (reported == null) {
							reported = new THashSet<>();
						}
						reported.add(connection);
					}
				}
				JDBCUtil.openConnections.removeAll(reported);
				try {
					Thread.sleep(10_000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public MAGA(DataSource dataSourceReadWrite, Cache cache, ObjectMapper objectMapper) {
		this.withDataSource(dataSourceReadWrite)
				.withCache(cache)
				.withObjectMapper(objectMapper);
	}

	public MAGA() {
	}

	/**
	 * Warning: for backwards compat, this constructor defaults to a HashMapCache
	 */
	public MAGA(DataSource dataSourceReadWrite) {
		this.withDataSource(dataSourceReadWrite)
				.withHashMapCache();
	}

	public MAGA(DataSource dataSourceReadWrite, Cache cache) {
		this.withDataSource(dataSourceReadWrite)
				.withCache(cache);
	}

	public MAGA(DataSource dataSourceReadWrite, Cache cache, MAGALoadTemplate loadTemplate) {
		this.withDataSource(dataSourceReadWrite)
				.withCache(cache)
				.withLoadTemplate(loadTemplate);
	}

	public MAGA(DataSource dataSourceReadWrite, Cache cache, MAGALoadTemplate loadTemplate, IDGen idGen) {
		this.withDataSource(dataSourceReadWrite)
				.withCache(cache)
				.withLoadTemplate(loadTemplate)
				.withIDGen(idGen);
	}

	public MAGA(DataSource dataSourceReadWrite, Cache cache, ObjectMapper objectMapper, MAGALoadTemplate loadTemplate, IDGen idGen) {
		this.withDataSource(dataSourceReadWrite)
				.withCache(cache)
				.withObjectMapper(objectMapper)
				.withLoadTemplate(loadTemplate)
				.withIDGen(idGen);
	}

	public MAGA withHashMapCache() {
		return withCache(new HashMapCache(1000));
	}

	public MAGA withIDGen(IDGen idGen) {
		this.idGen = idGen;
		return this;
	}

	public MAGA withLoadTemplate(MAGALoadTemplate loadTemplate) {
		this.loadTemplate = loadTemplate;
		return this;
	}

	public MAGA withCache(Cache cache) {
		this.cache = MAGACache.getInstance(cache);
		return this;
	}

	public MAGA withCache(MAGACache cache) {
		this.cache = cache;
		return this;
	}

	public MAGA withDataSource(DataSource dataSourceReadWrite) {
		return this
				.withDataSourceRead(dataSourceReadWrite)
				.withDataSourceWrite(dataSourceReadWrite);
	}

	public MAGA withDataSourceWrite(DataSource dataSourceWrite) {
		this.dataSourceWrite = dataSourceWrite;
		return this;
	}

	public MAGA withDataSourceRead(DataSource dataSourceRead) {
		this.dataSourceRead = dataSourceRead;
		return this;
	}

	public MAGA withObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		// This is totally not thread-safe, but MAGA relies on JSONUtil for persistence, and
		// I want to be able to customize JSONUtil from user code, so massive TODO here to make
		// JSONUtil instance-scoped. -alex
		if (objectMapper != null) {
			JSONUtil.objectMapper = objectMapper;
		}
		return this;
	}

	public MAGA withWriteThroughCacheOnUpdate(boolean writeThroughCacheOnUpdate) {
		this.writeThroughCacheOnUpdate = writeThroughCacheOnUpdate;
		return this;
	}

	public MAGA withDefaultCharacterSet(String defaultCharacterSet) {
		this.defaultCharacterSet = defaultCharacterSet;
		return this;
	}

	public MAGA withDefaultCollate(String defaultCollate) {
		this.defaultCollate = defaultCollate;
		return this;
	}

	public String getDefaultCharacterSet() {
		return defaultCharacterSet;
	}

	public String getDefaultCollate() {
		return defaultCollate;
	}

	public boolean isWriteThroughCacheOnUpdate() {
		return writeThroughCacheOnUpdate;
	}

	public <T extends MAGAObject> T load(Class<T> clazz, long id) {
		return clazz.cast(buildObjectLoad().load(clazz, id));
	}

	public <T extends MAGAObject> List<T> load(Class<T> clazz, Collection<Long> ids) {
		return buildObjectLoad().load(clazz, ids);
	}

	public <T extends MAGAObject> List<T> loadAll(Class<T> clazz) {
		return buildObjectLoad().loadAll(clazz);
	}

	public <T extends MAGAObject> List<T> loadWhereExtra(Class<T> clazz, String where, String extra, Object... params) {
		return buildObjectLoad().loadWhereExtra(clazz, where, extra, params);
	}

	public <T extends MAGAObject> List<T> loadByIndex(Class<T> clazz, String columnName, Object value) {
		List<Long> ids = loadIdsByIndex(clazz, columnName, value);
		return load(clazz, ids);
	}

	public <T extends MAGAObject> T loadByIndexSingle(Class<T> clazz, String columnName, Object value) {
		List<T> objects = load(clazz, loadIdsByIndex(clazz, columnName, value));
		return objects.isEmpty() ? null : objects.get(0);
	}

	public <T extends MAGAObject> List<Long> loadIdsByIndex(Class<T> clazz, String columnName, Object value) {
		return buildIndexLoad().load(clazz, columnName, value);
	}

	public List<MAGAObject> loadTemplate(MAGALoadTemplate template) {
		return buildObjectLoad().loadTemplate(template);
	}

	public void save(MAGAObject toSave) {
		throwExceptionIfCantSave(toSave);
		buildObjectUpdate().update(toSave);
	}

	public void delete(MAGAObject toDelete) {
		throwExceptionIfCantSave(toDelete);
		throwExceptionIfObjectUnsaved(toDelete);
		buildObjectDelete().delete(toDelete);
	}

	public List loadAssociatedObjects(MAGAObject baseObject, MAGAAssociation association) {
		throwExceptionIfObjectUnsaved(baseObject);
		return buildAssociationLoad().load(baseObject, association);
	}

	public void addAssociation(MAGAObject baseObject, MAGAObject otherObject, MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		buildAssociationAdd().add(baseObject, otherObject, association);
	}

	public void deleteAssociations(MAGAObject baseObject, MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfObjectUnsaved(baseObject);
		buildAssociationDelete().delete(baseObject, association);
	}

	public void deleteAssociation(MAGAObject baseObject, MAGAObject otherObject, MAGAAssociation association) {
		throwExceptionIfCantSave(baseObject);
		throwExceptionIfCantSave(otherObject);
		throwExceptionIfObjectUnsaved(baseObject);
		buildAssociationDelete().delete(baseObject, otherObject, association);
	}

	public void schemaSync() {
		new SchemaSync(this).go();
	}

	private void throwExceptionIfCantSave(MAGAObject object) {
		if (object.templateAssociations != null) {
			throw new MAGAException("Templates and objects returned from templates are read-only");
		}
	}

	private void throwExceptionIfObjectUnsaved(MAGAObject object) {
		if (object.id == 0) {
			throw new MAGAException(
					"Method unsupported for unsaved object [" + object.getClass().getName() + "] " + object.id);
		}
	}

	public List<MAGAAssociation> loadWhereHasClassWithJoinColumn(Class<? extends MAGAObject> class1) {
		return buildAssociationLoad().loadWhereHasClassWithJoinColumn(class1);
	}

	public List loadWhereHasClass(Class clazz) {
		return buildAssociationLoad().loadWhereHasClass(clazz);
	}

	public void dirtyObject(MAGAObject object) {
		cache.dirtyObject(object);
	}

	public void dirtyAssociation(MAGAObject object, MAGAAssociation association) {
		cache.dirtyAssoc(object, association);
	}

	public <T extends MAGAObject> List<T> loadWhere(Class<T> clazz, String where, Object... params) {
		return buildAssociationLoad().loadWhere(clazz, where, params);
	}

	private ObjectLoad objectLoad;
	public ObjectLoad buildObjectLoad() {
		if (objectLoad == null) {
			objectLoad = new ObjectLoad(this);
		}
		return objectLoad;
	}

	private AssociationLoad associationLoad;
	public AssociationLoad buildAssociationLoad() {
		if (associationLoad == null) {
			associationLoad = new AssociationLoad(this);
		}
		return associationLoad;
	}

	private ObjectUpdate objectUpdate;
	public ObjectUpdate buildObjectUpdate() {
		if (objectUpdate == null) {
			objectUpdate = new ObjectUpdate(this);
		}
		return objectUpdate;
	}

	private AssociationAdd associationAdd;
	public AssociationAdd buildAssociationAdd() {
		if (associationAdd == null) {
			associationAdd = new AssociationAdd(this);
		}
		return associationAdd;
	}

	private AssociationDelete associationDelete;
	public AssociationDelete buildAssociationDelete() {
		if (associationDelete == null) {
			associationDelete = new AssociationDelete(this);
		}
		return associationDelete;
	}

	private IndexLoad indexLoad;
	public IndexLoad buildIndexLoad()  {
		if (indexLoad == null) {
			indexLoad = new IndexLoad(this);
		}
		return indexLoad;
	}

	private ObjectDelete objectDelete;
	public ObjectDelete buildObjectDelete() {
		if (objectDelete == null) {
			objectDelete = new ObjectDelete(this);
		}
		return objectDelete;
	}
}
