package com.ericdmartell.maga;

import com.ericdmartell.maga.utils.JDBCUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SchemaSyncTest extends BaseMAGATest {

    @Test
    public void testCharSet() {
        JDBCUtil.executeUpdate("DROP TABLE Obj1", getDataSource());
        MAGA maga = new MAGA().withDataSource(getDataSource()).withDefaultCharacterSet("utf8mb4").withDefaultCollate("utf8mb4_unicode_ci");
        maga.schemaSync();

        List<String> charSets = JDBCUtil.executeQueryAndReturnStrings(getDataSource(),
                "SELECT CCSA.character_set_name FROM information_schema.`TABLES` T,\n" +
                "information_schema.`COLLATION_CHARACTER_SET_APPLICABILITY` CCSA\n" +
                "WHERE CCSA.collation_name = T.table_collation\n" +
                "  AND T.table_schema = \"simpleorm\"\n" +
                "  AND T.table_name = \"Obj1\"");

        Assert.assertEquals(1, charSets.size());
        Assert.assertEquals("utf8mb4", charSets.get(0));
    }
}
