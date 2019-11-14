package com.ericdmartell.maga;

import com.ericdmartell.maga.annotations.MAGAORMField;
import com.ericdmartell.maga.associations.MAGAAssociation;
import com.ericdmartell.maga.id.RandomIDGen;
import com.ericdmartell.maga.objects.MAGAObject;
import com.ericdmartell.maga.objects.MAGASQLSerializer;
import com.ericdmartell.maga.utils.JSONUtil;
import gnu.trove.map.hash.THashMap;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SerializerTest extends BaseMAGATest {

	public static class TestSerializer extends MAGASQLSerializer {

		@Override
		public void setFieldValue(MAGAObject obj, Field field, Class fieldClass, String value) {
			if (value == null) {
				return;
			}
			Map<String, Object> map = JSONUtil.stringToMap(value);

			Map<String, A> target = new THashMap<>();
			for (String key : map.keySet()) {
				if ("A".equals(key)) {
					target.put("A", JSONUtil.stringToObject(JSONUtil.mapToString((Map)map.get(key)), A.class));
				}
				if ("B".equals(key)) {
					target.put("B", JSONUtil.stringToObject(JSONUtil.mapToString((Map)map.get(key)), B.class));
				}
				if ("C".equals(key)) {
					target.put("C", JSONUtil.stringToObject(JSONUtil.mapToString((Map)map.get(key)), C.class));
				}
			}
			try {
				field.set(obj, target);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String getFieldValue(MAGAObject obj, Field field, Class fieldType) {
			try {
				return JSONUtil.serializableToString(field.get(obj));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class A {
		public String a;
	}

	public static class B extends A {
		public String b;
	}

	public static class C extends A {
		public String c;
	}

	public static class TestObject extends MAGAObject<TestObject> {
		@MAGAORMField(serializer = TestSerializer.class)
		Map<String, A> data;
	}

	@Test
	public void test() {
		MAGA orm = new MAGA()
				.withDataSource(getMAGA().dataSourceWrite)
				.withHashMapCache()
				.withIDGen(new RandomIDGen());

		TestObject one = new TestObject();
		one.data = new HashMap<String, A>();
		B b = new B();
		b.a = "alex";
		b.b = "brett";

		C c = new C();
		c.a = "allen";
		c.c = "chris";
		one.data.put("B", b);
		one.data.put("C", c);

		orm.save(one);

		TestObject oneLoaded = orm.load(TestObject.class, one.id);
		A bLoaded = oneLoaded.data.get("B");
		A cLoaded = oneLoaded.data.get("C");
		Assert.assertTrue(bLoaded instanceof B);
		Assert.assertTrue(cLoaded instanceof C);
		Assert.assertEquals("brett", ((B)bLoaded).b);
		Assert.assertEquals("chris", ((C)cLoaded).c);
		Assert.assertEquals("alex", bLoaded.a);


	}

}
