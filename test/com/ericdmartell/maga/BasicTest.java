package com.ericdmartell.maga;

import com.ericdmartell.maga.annotations.MAGATimestampID;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.id.LongUUIDGen;
import com.ericdmartell.maga.id.RandomIDGen;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.utils.JDBCUtil;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import net.spy.memcached.MemcachedClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


public class BasicTest extends BaseMAGATest {

	@Test
	public void deleteOneToManyJoinsFromManySide() {
		MAGA orm = getMAGA();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);

		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);

		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(((MAGAObject)orm.loadAssociatedObjects(obj1, assoc).get(0)).id, obj2.id);
		Assert.assertEquals(((MAGAObject)orm.loadAssociatedObjects(obj2, assoc).get(0)).id, obj1.id);

		orm.deleteAssociations(obj2, assoc);

		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());

	}


	@Test
	public void reflectionWorksWhenAddingAssocFromManySide() {
		MAGA orm = getMAGA();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);

		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);

		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj2, obj1, assoc);
		Assert.assertEquals(((MAGAObject)orm.loadAssociatedObjects(obj1, assoc).get(0)).id, obj2.id);
		Assert.assertEquals(((MAGAObject)orm.loadAssociatedObjects(obj2, assoc).get(0)).id, obj1.id);
		Assert.assertEquals(obj2.joinColumn, obj1.id);

	}

	@Test
	public void assocWorksWhenModifyingJoinColumn() {
		MAGA orm = getMAGA();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);

		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);

		Obj2 obj2Other = new Obj2();
		obj2Other.field2 = "This is a test of field two (other)";
		orm.save(obj2Other);

		MAGAAssociation assoc = new TestAssoc2();
		orm.addAssociation(obj1, obj2Other, assoc);

		Assert.assertTrue(((MAGAObject)orm.loadAssociatedObjects(obj1, assoc).get(0)).id == obj2Other.id);
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());

		obj2.joinColumn = obj1.id;
		orm.save(obj2);


		Assert.assertEquals(orm.loadAssociatedObjects(obj1, assoc).size(), 2);
		Assert.assertEquals(((MAGAObject)orm.loadAssociatedObjects(obj2, assoc).get(0)).id, obj1.id);


	}
	@Test
	public void deleteManyToManyJoinsLeavingOtherAssocs() {
		MAGA orm = getMAGA();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);

		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);

		Obj2 obj3 = new Obj2();
		obj3.field2 = "This is a test of field two (other)";
		orm.save(obj3);

		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		orm.addAssociation(obj3, obj1, assoc);


		orm.deleteAssociations(obj2, assoc);

		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).size() == 1);
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());

	}
	@Test
	public void deleteManyToManyJoinLeavingOtherAssocs() {
		MAGA orm = getMAGA();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);

		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);

		Obj2 obj3 = new Obj2();
		obj3.field2 = "This is a test of field two (Other)";
		orm.save(obj3);

		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		orm.addAssociation(obj3, obj1, assoc);


		orm.deleteAssociation(obj2, obj1, assoc);

		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).size() == 1);
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());
	}

	@Test
	public void deleteManyToManyJoinsLeavingNoAssocs() {
		MAGA orm = getMAGA();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);

		Obj2 obj2 = new Obj2();
		obj2.field2 = "This is a test of field two";
		orm.save(obj2);

		Obj2 obj3 = new Obj2();
		obj3.field2 = "This is a test of field two (other)";
		orm.save(obj3);

		MAGAAssociation assoc = new TestAssoc();
		orm.addAssociation(obj2, obj1, assoc);
		orm.addAssociation(obj3, obj1, assoc);


		orm.deleteAssociations(obj1,  assoc);

		Assert.assertTrue(orm.loadAssociatedObjects(obj1, assoc).isEmpty());
		Assert.assertTrue(orm.loadAssociatedObjects(obj2, assoc).isEmpty());

	}

	@Test
	public void loadWhere() {
		MAGA orm = getMAGA();
		Obj1 obj1 = new Obj1();
		obj1.field1 = "This is a test of field one";
		orm.save(obj1);

		Assert.assertFalse(orm.loadAll(Obj1.class).isEmpty());
		Assert.assertFalse(orm.loadWhereExtra(Obj1.class, "1", "LIMIT 1").isEmpty());
		Assert.assertFalse(orm.loadWhereExtra(Obj1.class, "field1 = ?", "LIMIT 1", obj1.field1).isEmpty());
		Assert.assertTrue(orm.loadWhereExtra(Obj1.class, "0", "LIMIT 1").isEmpty());
	}

	@Test
	public void testBigDecimal() {
		MAGA orm = getMAGA();
		Obj3 obj = new Obj3();
		obj.val = new BigDecimal(3.25);
		orm.save(obj);
		Assert.assertTrue(((Obj3) orm.load(Obj3.class, obj.id)).val.compareTo(new BigDecimal(3.25)) == 0) ;

	}

	@Test
	public void testEnum() {
		MAGA orm = getMAGA();
		Obj3 obj = new Obj3();
		orm.save(obj);

		obj = orm.load(Obj3.class, obj.id);
		Assert.assertNull(obj.enumTest);

		obj.enumTest = Obj3.EnumTest.one;
		orm.save(obj);

		obj = orm.load(Obj3.class, obj.id);
		Assert.assertEquals(Obj3.EnumTest.one, obj.enumTest);

		obj.enumTest = null;
		orm.save(obj);
		obj = orm.load(Obj3.class, obj.id);
		Assert.assertNull(obj.enumTest);
	}

	@Test
	public void testClass() throws SQLException {
		MAGA orm = getMAGA();
		Obj3 obj = new Obj3();
		obj.classTest = Obj2.class;
		orm.save(obj);
		ResultSet rst = JDBCUtil.executeQuery(JDBCUtil.getConnection(getMAGA().dataSourceRead), "SELECT classTest FROM Obj3 WHERE id = ?", obj.id);
		rst.next();
		String actualSavedVal = rst.getString(1);
		Assert.assertEquals("com.ericdmartell.maga.Obj2", actualSavedVal);

		obj = orm.load(Obj3.class, obj.id);
		Assert.assertEquals(Obj2.class, obj.classTest);
	}

	@MAGATimestampID
	public static class IdGenEnt extends MAGAObject {}

	public static class NoIdGenEnt extends MAGAObject {}

	@Test
	public void testIds() throws SQLException {
		long before = System.currentTimeMillis();
		MAGA orm = new MAGA()
			.withDataSource(getMAGA().dataSourceWrite)
			.withHashMapCache()
			.withIDGen(new RandomIDGen());
		IdGenEnt obj1 = new IdGenEnt();
		orm.save(obj1);
		IdGenEnt obj2 = new IdGenEnt();
		orm.save(obj2);
		long after = System.currentTimeMillis();

		System.out.println(obj1.id + " " + obj2.id);
		Assert.assertTrue(Math.abs(obj1.id - obj2.id) > 1);
		long fakeUuidTimestamp = new LongUUIDGen(1).getJavaTimestamp(obj1.id);
		Assert.assertFalse(before <= fakeUuidTimestamp && after >= fakeUuidTimestamp);

		// test given id
		IdGenEnt obj3 = new IdGenEnt();
		long testId = System.currentTimeMillis();
		obj3.id = testId;
		orm.buildObjectUpdate().addSQL(obj3);
		Assert.assertNotNull(orm.load(IdGenEnt.class, testId));

		orm = orm.withIDGen(new LongUUIDGen(1));
		IdGenEnt obj4 = new IdGenEnt();
		orm.save(obj4);
		after = System.currentTimeMillis();

		long timestamp = ((LongUUIDGen)orm.idGen).getJavaTimestamp(obj4.id);
		Assert.assertTrue(before <= timestamp && after >= timestamp);

		// test given id
		IdGenEnt obj5 = new IdGenEnt();
		testId = System.currentTimeMillis();
		obj5.id = testId;
		orm.buildObjectUpdate().addSQL(obj5);
		Assert.assertNotNull(orm.load(IdGenEnt.class, testId));

		NoIdGenEnt obj6 = new NoIdGenEnt();
		orm.save(obj6);
		NoIdGenEnt obj7 = new NoIdGenEnt();
		orm.save(obj7);
		Assert.assertEquals(1, obj7.id - obj6.id);

		// test given id
		IdGenEnt obj8 = new IdGenEnt();
		testId = System.currentTimeMillis();
		obj8.id = testId;
		orm.buildObjectUpdate().addSQL(obj8);
		Assert.assertNotNull(orm.load(IdGenEnt.class, testId));
	}



}