package com.ericdmartell.maga.utils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.*;

import com.ericdmartell.maga.annotations.MAGAORMField;
import com.ericdmartell.maga.objects.MAGAObject;

import com.ericdmartell.maga.objects.MAGASQLSerializer;
import gnu.trove.map.hash.THashMap;

public class ReflectionUtils {
	// An in-memory cache because inspection is slow.

	private static Map<Class<MAGAObject>, Map<String, Field>> classesToFieldNamesToFields   = new THashMap<>();
	private static Map<Class<MAGAObject>, Map<String, Class>> classesToFieldNamesAndTypes   = new THashMap<>();
	private static Map<Class, List<String>>                   classesToSQLIndexedFieldNames = new THashMap<>();
	private static Map<Class, List<String>>                   classesToCacheIndexedFieldNames = new THashMap<>();
	private static Map<Class<MAGAObject>, Map<String, Class<? extends MAGASQLSerializer>>> classesToFieldNamesAndSerializers = new THashMap<>();
	public static Set<Class>                                  standardClasses               = new HashSet<>(Arrays.asList(new Class[] {
		int.class, Integer.class, BigDecimal.class, String.class, long.class, Long.class, Date.class, Boolean.class, boolean.class
	}));

	public static Collection<String> getFieldNames(Class clazz) {
		// Lazily populating classesToFieldNamesToFields since 2016.
		if (!classesToFieldNamesAndTypes.containsKey(clazz)) {
			buildIndex(clazz);
		}

		return classesToFieldNamesAndTypes.get(clazz).keySet();
	}

	public static Class getFieldType(Class clazz, String fieldName) {
		if (!classesToFieldNamesAndTypes.containsKey(clazz)) {
			buildIndex(clazz);
		}
		return classesToFieldNamesAndTypes.get(clazz).get(fieldName);
	}
			
	public static boolean setFieldValue(MAGAObject obj, String fieldName, Object value) {
		if (!classesToFieldNamesAndTypes.containsKey(obj.getClass())) {
			buildIndex(obj.getClass());
		}
		try {
			Field field = classesToFieldNamesToFields.get(obj.getClass()).get(fieldName);
			Class fieldClass = getFieldType(obj.getClass(), fieldName);
			Class<? extends MAGASQLSerializer> serializerClass = classesToFieldNamesAndSerializers.get(obj.getClass()).get(fieldName);

			if (field == null) {
				return false;
			}
			if (serializerClass != null) {
				serializerClass.newInstance().setFieldValue(obj, field, fieldClass, String.valueOf(value));
			} else if (fieldClass.equals(BigDecimal.class)) {
				if (value == null) {
					field.set(obj, value);
				} else {
					field.set(obj, new BigDecimal(value + ""));
				}
			} else if (fieldClass.equals(long.class) || fieldClass.equals(Long.class)) {
				if (value == null) {
					value = 0L;
				}
				field.set(obj, ((Number) value).longValue());
			} else if (fieldClass.equals(int.class) || fieldClass.equals(Integer.class)) {
				if (value == null) {
					value = 0;
				}
				field.set(obj, ((Number) value).intValue());
			} else if (fieldClass.equals(String.class) && value instanceof Number) {
				field.set(obj, value + "");
			} else if ((fieldClass.equals(Boolean.class) || fieldClass.equals(boolean.class)) && value instanceof String) {
				field.set(obj, value == null ? false : ("1".equals(value + "")));
			} else if (value != null && (fieldClass.isEnum())) {
				field.set(obj, Enum.valueOf(fieldClass, String.valueOf(value)));
			} else if (value != null && fieldClass.equals(Class.class)) {
				field.set(obj, Class.forName(String.valueOf(value)));
			} else if (value != null && !standardClasses.contains(fieldClass) && Collection.class.isAssignableFrom(fieldClass)) {
				field.set(obj, JSONUtil.stringToList(value + ""));
			} else if (value != null && !standardClasses.contains(fieldClass)) {
				field.set(obj, JSONUtil.stringToObject(value + "", fieldClass));
			} else {
				field.set(obj, value);
			}
		} catch (IllegalArgumentException | IllegalAccessException | ClassNotFoundException | InstantiationException e) {
			throw new MAGAException(e);
		}

		return true;
	}

	public static Object getFieldValue(MAGAObject obj, String fieldName) {
		try {
		if (!classesToFieldNamesAndTypes.containsKey(obj.getClass())) {
			buildIndex(obj.getClass());
		}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		Object ret;
		Field field = classesToFieldNamesToFields.get(obj.getClass()).get(fieldName);
		Class<? extends MAGASQLSerializer> serializerClass = classesToFieldNamesAndSerializers.get(obj.getClass()).get(fieldName);
		Class fieldType = getFieldType(obj.getClass(), fieldName);
		try {
			if (field == null) {
				ret = null;
			} else {
				if (serializerClass != null) {
					ret = serializerClass.newInstance().getFieldValue(obj, field, fieldType);
				} else {
					ret = field.get(obj);
					if (ret != null && !standardClasses.contains(fieldType) && !fieldType.isEnum() && !fieldType.equals(Class.class)) {
						if (Collection.class.isAssignableFrom(getFieldType(obj.getClass(), fieldName))) {
							ret = JSONUtil.listToString((List) ret);
						} else {
							ret = JSONUtil.serializableToString(ret);
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			throw new MAGAException(e);
		}
		if (fieldType == null) {
			return ret;
		}


		if (ret == null) {
			if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
				ret = 0L;
			} else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
				ret = 0;
			} else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
				ret = false;
			} else {
				return null;
			}
		}

		if (fieldType.isEnum()) {
			return String.valueOf(ret);
		} else if (fieldType.equals(Class.class)) {
			return ((Class)ret).getName();
		} else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
			return ((boolean)ret) ? "1" : "0";
		}
		return ret;

	}

	public static List<String> getSQLIndexedColumns(Class clazz) {
		if (!classesToFieldNamesAndTypes.containsKey(clazz)) {
			buildIndex(clazz);
		}
		return classesToSQLIndexedFieldNames.get(clazz);
	}

	public static List<String> getCacheIndexedColumns(Class clazz) {
		if (!classesToFieldNamesAndTypes.containsKey(clazz)) {
			buildIndex(clazz);
		}
		return classesToCacheIndexedFieldNames.get(clazz);
	}

	public static List<Field> getAllFields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		fields.addAll(Arrays.asList(type.getDeclaredFields()));
	    if (type.getSuperclass() != null) {
	        fields.addAll(getAllFields(type.getSuperclass()));
	    }
	    return fields;
	}
	
	private static void buildIndex(Class clazz) {
		
		Map<String, Field>                              fieldNamesToField           = new THashMap<>();
		Map<String, Class>                              fieldNamesToType            = new THashMap<>();
		Map<String, Class<? extends MAGASQLSerializer>> fieldNamesToSerializerClass = new THashMap<>();

		List<String> sqlIndexedColumns = new ArrayList<>();
		List<String> cacheIndexedColumns = new ArrayList<>();

		for (Field field : getAllFields(clazz)) {
			
			field.setAccessible(true);
			if (field.isAnnotationPresent(MAGAORMField.class)) {
				fieldNamesToField.put(field.getName(), field);
				fieldNamesToType.put(field.getName(), field.getType());
				MAGAORMField anno = field.getAnnotation(MAGAORMField.class);
				if (anno.isSQLIndex()) {
					sqlIndexedColumns.add(field.getName());
				}
				if (anno.isCacheIndex()) {
					cacheIndexedColumns.add(field.getName());
				}

				if (anno.serializer() != null && anno.serializer() != MAGASQLSerializer.class) {
					fieldNamesToSerializerClass.put(field.getName(), anno.serializer());
				}
			}
		}
		fieldNamesToType.put("id", long.class);
		try {
			fieldNamesToField.put("id", clazz.getField("id"));
		} catch (NoSuchFieldException | SecurityException e) {
			throw new MAGAException(e);
		}
		classesToSQLIndexedFieldNames.put(clazz, sqlIndexedColumns);
		classesToCacheIndexedFieldNames.put(clazz, cacheIndexedColumns);
		classesToFieldNamesToFields.put(clazz, fieldNamesToField);
		classesToFieldNamesAndTypes.put(clazz, fieldNamesToType);
		classesToFieldNamesAndSerializers.put(clazz, fieldNamesToSerializerClass);
	}

	public static <T extends MAGAObject> T getEntityFromResultSet(Class<T> clazz, ResultSet rst) {
		try {
			T toFill =clazz.newInstance();
			List<String> fieldNames = new ArrayList<>(ReflectionUtils.getFieldNames(clazz));
			for (String fieldName : fieldNames) {
				ReflectionUtils.setFieldValue(toFill, fieldName, rst.getObject(fieldName));
			}
			return toFill;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
}
