package com.ericdmartell.maga;

import com.ericdmartell.maga.actions.DataMigrate;
import com.ericdmartell.maga.annotations.MAGADataMigration;
import com.ericdmartell.maga.utils.JDBCUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Created by alexwyler on 8/11/18.
 */
public class MigrationTest extends BaseMAGATest {

    static Connection conn;

    @BeforeClass
    public static void setup() {
        MAGA maga = getMAGA();
        conn = JDBCUtil.getConnection(maga.dataSourceWrite);
        JDBCUtil.executeUpdate(conn, "DROP TABLE IF EXISTS Migration_Test_Table");
        JDBCUtil.executeUpdate(conn, "DROP TABLE IF EXISTS Data_Migration_Record");
        maga.schemaSync();
    }

    @MAGADataMigration(order = "0001")
    public static void staticMigration() {
        JDBCUtil.executeUpdate(conn, "CREATE TABLE `Migration_Test_Table` (id varchar(255))");
    }

    @MAGADataMigration(order = "0002")
    public void nonStaticMigration() {
        JDBCUtil.executeQuery(conn, "SELECT * FROM `Migration_Test_Table`");
    }

    @MAGADataMigration()
    public void zzzzzz_defaultOrderMigration() {
        JDBCUtil.executeQuery(conn, "SELECT * FROM `Migration_Test_Table`");
    }

    @MAGADataMigration(order = "0003")
    public void aaaaa_outOfOrderMigration() {
        JDBCUtil.executeQuery(conn, "SELECT * FROM `Migration_Test_Table`");
    }

    @Test
    public void testMigration() {
        new DataMigrate(getMAGA(), "com.ericdmartell.maga").go();
        JDBCUtil.executeQuery(conn, "SELECT * FROM `Migration_Test_Table`");
    }
}
