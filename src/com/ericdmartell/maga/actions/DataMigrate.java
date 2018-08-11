package com.ericdmartell.maga.actions;

import com.ericdmartell.maga.MAGA;
import com.ericdmartell.maga.annotations.MAGADataMigration;
import com.ericdmartell.maga.objects.DataMigrationRecord;
import com.ericdmartell.maga.objects.MAGAObject;
import com.sun.tools.javac.util.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Runs migration methods annotated with @MAGADataMigration("lexicographical ordering string").  SchemaSync must be
 * run prior to this action.
 */
public class DataMigrate {

    MAGA maga;
    String classPackage;

    public DataMigrate(MAGA maga, String classPackage) {
        this.maga = maga;
        this.classPackage = classPackage;
    }

    public void go() {
        Reflections  reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(classPackage))
                        .setScanners(new MethodAnnotationsScanner()));
        List<Method> methods     = new ArrayList<>();
        methods.addAll(reflections.getMethodsAnnotatedWith(MAGADataMigration.class));
        methods.sort(Comparator.comparing(this::getOrder));

        for (Method m : methods) {
            List<DataMigrationRecord> records = maga.loadWhere(DataMigrationRecord.class, "name = ?", m.getName());
            DataMigrationRecord record;
            if (!records.isEmpty()) {
                record = records.get(0);
                if (record.end != null) {
                    continue;
                }
                System.out.println("Re-starting migration " + m.getName());
            } else {
                record = new DataMigrationRecord();
                record.name = m.getName();
                record.order = getOrder(m);
                System.out.println("Starting migration " + m.getName());
            }
            record.start = new Date();
            maga.save(record);
            m.setAccessible(true);
            Object invokeOn = null;
            try {
                if (!Modifier.isStatic(m.getModifiers())) {
                    invokeOn = m.getDeclaringClass().newInstance();
                }
                m.invoke(invokeOn);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            record.end = new Date();
            maga.save(record);
            System.out.println("Completed migration " + m.getName());
        }
    }

    public String getOrder(Method m) {
        MAGADataMigration anno = m.getAnnotation(MAGADataMigration.class);
        String order = anno.order();
        if (order.isEmpty()) {
            order = m.getName();
        }
        return order;
    }
}
