package com.ericdmartell.maga.objects;

import com.ericdmartell.maga.utils.JSONUtil;
import com.ericdmartell.maga.utils.MAGAException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public abstract class MAGASQLSerializer {

    public abstract void setFieldValue(MAGAObject obj, Field field, Class fieldClass, String value);

    public abstract String getFieldValue(MAGAObject obj, Field field,  Class fieldType);
}
