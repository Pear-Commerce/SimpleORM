package com.ericdmartell.maga.actions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.annotations.MAGATimestampID;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.cache.MAGACache;
import com.ericdmartell.maga.id.IDGen;
import com.ericdmartell.maga.objects.MAGALoadTemplate;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.*;

import gnu.trove.map.hash.THashMap;

public class ObjectUpdate extends MAGAAction {

    private static Map<Class, Boolean> autoId = new THashMap<>();

    public ObjectUpdate(DataSource dataSource, MAGACache cache, MAGA maga, MAGALoadTemplate template) {
        super(dataSource, cache, maga, template);
    }

    public void update(final MAGAObject obj) {
        // Object for history
        MAGAObject            oldObj               = null;
        List<MAGAAssociation> affectedAssociations = maga.loadWhereHasClassWithJoinColumn(obj.getClass());
        if (obj.id == 0) {
            // We're adding an object without an assigned id
            addSQL(obj);
            cache.dirtyObject(obj);
        } else {
            // TODO(alex): we can replace this with keeping a set of pristine join fields
            oldObj = maga.load(obj.getClass(), obj.id);
            // If the object being updated has a join column, an old object
            // might have been joined with it. We
            // Dirty those assocs.
            for (MAGAAssociation assoc : affectedAssociations) {
                biDirectionalDirty(obj, assoc);
            }
            dirtyIndexes(obj);
            // Update the db after we've done our dirtying.
            updateSQL(obj);
            cache.dirtyObject(obj);
            obj.savePristineIndexValues();
        }

        for (MAGAAssociation assoc : affectedAssociations) {
            long val    = 0;
            long oldVal = 0;
            try {
                val = (long) ReflectionUtils.getFieldValue(obj, assoc.class2Column());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (oldObj != null) {
                oldVal = (long)ReflectionUtils.getFieldValue(oldObj, assoc.class2Column());
            }
            if (val != 0 && (oldObj == null || oldVal == 0 || oldVal != (val))) {
                // We have a new assoc... the object on the other side needs to
                // have its assoc pointing at this one dirtied.
                biDirectionalDirty(obj, assoc);
            }

        }
        HistoryUtil.recordHistory(oldObj, obj, maga, dataSource);
    }

    /**
     * Dirty the previous index values and the current index values for an object.
     */
    private void dirtyIndexes(MAGAObject obj) {
        IndexLoad                      indexAction           = new IndexLoad(dataSource, cache, maga, null);
        Map<String, Object> pristineIndexValues = obj.getPristineIndexValues();
        for (String indexName : ReflectionUtils.getIndexedColumns(obj.getClass())) {
            Object currentValue = ReflectionUtils.getFieldValue(obj, indexName);
            indexAction.dirty(obj.getClass(), indexName, currentValue);

            // minor optimization for newly-created objects (to avoid unnecessary dirtying of 'null' values)
            if (pristineIndexValues.containsKey(indexName)) {
                Object pristineValue = pristineIndexValues.get(indexName);
                indexAction.dirty(obj.getClass(), indexName, pristineValue);
            }
        }

    }

    private void biDirectionalDirty(MAGAObject obj, MAGAAssociation association) {

        List<MAGAObject> otherSide = maga.loadAssociatedObjects(obj, association);
        cache.dirtyObject(obj);
        for (MAGAObject other : otherSide) {
            cache.dirtyAssoc(other, association);
        }
        cache.dirtyAssoc(obj, association);

    }

    public void addSQL(MAGAObject obj) {
        Class clazz = obj.getClass();
        if (!autoId.containsKey(clazz)) {
            autoId.put(clazz, clazz.isAnnotationPresent(MAGATimestampID.class));
        }

        boolean genId = autoId.get(clazz);
        
        List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(obj.getClass()));

        if (obj.id == 0) {
            if (genId && maga.idGen != null) {
                obj.id = maga.idGen.getNext();
            }
        }

        String sql          = "insert into `" + obj.getClass().getSimpleName() + "` (";
        String fieldNameSql = "";
        int    numFields    = 0;
        for (String fieldName : fieldNames) {
            if (fieldName.equals("id") && obj.id == 0) {
                continue;
            }
            if (!fieldNameSql.isEmpty()) {
                fieldNameSql += ", ";
            }
            fieldNameSql += "`" + fieldName + "`";
            numFields++;
        }

        sql += fieldNameSql + ") values (";

        String fieldPlaceholders = "";
        for (int i = 0; i < numFields; i++) {
            if (!fieldPlaceholders.isEmpty()) {
                fieldPlaceholders += ", ";
            }
            fieldPlaceholders += "?";
        }

        sql += fieldPlaceholders + ")";

        Connection con = JDBCUtil.getConnection(dataSource);
        PreparedStatement pstmt = null;
        try {
            pstmt = JDBCUtil.prepareStatmenent(con, sql);

            int i = 1;
            for (String fieldName : fieldNames) {
                if (fieldName.equals("id") && obj.id == 0) {
                    continue;
                }
                pstmt.setObject(i++, ReflectionUtils.getFieldValue(obj, fieldName));
            }

            pstmt.executeUpdate();
            if (obj.id == 0) {
                ResultSet rst = pstmt.executeQuery("SELECT LAST_INSERT_ID()");
                rst.first();
                obj.id = rst.getLong(1);
            }

            JDBCUtil.updates++;

        } catch (SQLException e) {
            throw new MAGAException(String.valueOf(pstmt), e);
        } finally {
            JDBCUtil.closeConnection(con);
        }

        dirtyIndexes(obj);
        obj.savePristineIndexValues();
    }

    private void updateSQL(MAGAObject obj) {
        List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(obj.getClass()));
        fieldNames.remove("id");
        if (fieldNames.isEmpty()) {
            return;
        }
        String sql = "update  `" + obj.getClass().getSimpleName() + "` set ";
        for (String fieldName : fieldNames) {
            sql += "`" + fieldName + "`" + "= ? ,";
        }
        sql = sql.substring(0, sql.length() - 1);
        sql += " where id = ?";

        Connection con = JDBCUtil.getConnection(dataSource);
        try {
            PreparedStatement pstmt = JDBCUtil.prepareStatmenent(con, sql);

            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                pstmt.setObject(i + 1, ReflectionUtils.getFieldValue(obj, fieldName));
            }
            pstmt.setLong(fieldNames.size() + 1, obj.id);
            pstmt.executeUpdate();
            JDBCUtil.updates++;
        } catch (SQLException e) {
            throw new MAGAException(e);
        } finally {
            JDBCUtil.closeConnection(con);
        }
    }
}
