package com.ericdmartell.maga.annotations;

/**
 * Created by alexwyler on 8/11/18.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MAGADataMigration {

    public String order() default "";
}
