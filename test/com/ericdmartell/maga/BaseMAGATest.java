package com.ericdmartell.maga;

import java.net.InetSocketAddress;

import javax.sql.DataSource;

import org.junit.BeforeClass;

import com.ericdmartell.maga.utils.JDBCUtil;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import net.spy.memcached.MemcachedClient;


public abstract class BaseMAGATest {
	private static DataSource      dataSource;
	private static MemcachedCache  mcCache;
	private static MAGA            basicMAGA;

	@BeforeClass
	public static void setUp() throws Exception {
		BaseMAGATest.dataSource = createDataSource();
		mcCache = new MemcachedCache(new MemcachedClient(new InetSocketAddress("localhost", 11211)));
		basicMAGA = new MAGA(dataSource);
		basicMAGA.schemaSync();
	}

	protected static MemcachedCache getMcCache() {
		return mcCache;
	}

	protected static MAGA getMAGA() {
		return basicMAGA;
	}

	public static DataSource getDataSource() {
		return dataSource;
	}

	private static DataSource createDataSource() {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser("root");
		String password = System.getenv("MYSQL_PASSWORD");
		if (password == null) {
			password = "Rockydog1";
		}
		dataSource.setPassword(password);
		dataSource.setServerName("localhost");
		try {
			JDBCUtil.executeUpdate("drop schema simpleorm", dataSource);
		} catch (RuntimeException e) {
			// Its ok if it doesn't exist
		} finally {
			JDBCUtil.executeUpdate("create schema simpleorm", dataSource);
		}
		dataSource = new MysqlDataSource();
		dataSource.setDatabaseName("simpleorm");
		dataSource.setUser("root");
		dataSource.setPassword(password);
		dataSource.setServerName("localhost");
		return dataSource;
	}
}