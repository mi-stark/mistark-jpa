package com.mistark.data.jpa.helper;

import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SoftDelHelper {

    private static Map<Class, Object> ValidVals = new ConcurrentHashMap<Class, Object>(){{
        put(int.class, 1);
        put(Integer.class, 1);
        put(boolean.class, 1);
        put(Boolean.class, 1);
        put(char.class, 'Y');
        put(String.class, "Y");
    }};

    private static Map<Class, Object> InValidVals = new ConcurrentHashMap<Class, Object>(){{
        put(int.class, 0);
        put(Integer.class, 0);
        put(boolean.class, 0);
        put(Boolean.class, 0);
        put(char.class, 'N');
        put(String.class, "N");
    }};

    private static Set<Integer> softDelMs = new HashSet<>();

    public static void addSoftDelStatement(MappedStatement ms){
        if(ms == null) return;
        softDelMs.add(ms.hashCode());
    }

    public static boolean isSoftDel(EntityMeta meta){
        return meta!=null && meta.isSoftDel();
    }

    public static boolean isSoftDel(MappedStatement ms){
        return ms!=null && softDelMs.contains(ms.hashCode());
    }

    public static Object getValue(boolean flag, EntityMeta meta){
        return getValue(flag, meta.getSoftDel().getJavaType());
    }

    public static Object getValue(boolean flag, Class type){
        Object value = (flag ? ValidVals: InValidVals).get(type);
        if(value == null){
            throw new BuilderException(String.format("wrong type as a delete flag \"%s\"", type.getName()));
        }
        return value;
    }
}
