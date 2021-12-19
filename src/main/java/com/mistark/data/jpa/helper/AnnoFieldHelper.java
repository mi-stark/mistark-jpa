package com.mistark.data.jpa.helper;

import com.mistark.data.jpa.annotation.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AnnoFieldHelper {

    private final static Map<Class<? extends Annotation>, String> AnnoFieldName = new ConcurrentHashMap<>();
    private final static Map<Class<? extends Annotation>, String> AnnoEnable = new ConcurrentHashMap<>();
    private final static Map<Class<? extends Annotation>, Class[]> AnnoFieldTypes = new ConcurrentHashMap<>();
    private final static List<Class<? extends Annotation>> AnnoTypes = new Vector<>();

    public final static Class[] ID_TYPES = new Class[]{Long.class, Integer.class, long.class, int.class, String.class};
    public final static Class[] NUM_TYPES = new Class[]{Long.class, Integer.class, long.class, int.class};
    public final static Class[] DATE_TYPES = new Class[]{Date.class, String.class};
    public final static Class[] BOOL_TYPES = new Class[]{Integer.class, int.class, boolean.class, String.class, char.class};

    static {
        String fieldKey = "field";
        String enableKey = "enable";
        register(Id.class, null, null, ID_TYPES);
        register(TenantId.class, fieldKey, enableKey, ID_TYPES);
        register(CreateBy.class, fieldKey, enableKey, ID_TYPES);
        register(CreateDate.class, fieldKey, enableKey, DATE_TYPES);
        register(UpdateBy.class, fieldKey, enableKey, ID_TYPES);
        register(UpdateDate.class, fieldKey, enableKey, DATE_TYPES);
        register(SoftDel.class, fieldKey, enableKey, BOOL_TYPES);
        register(Version.class, fieldKey, enableKey, NUM_TYPES);
    }

    public static void register(Class<? extends Annotation> annoType, String field, String enable, Class[] types){
        if(annoType==null) return;
        if(StringUtils.isNotEmpty(field)) AnnoFieldName.put(annoType, field);
        if(StringUtils.isNotEmpty(enable)) AnnoEnable.put(annoType, enable);
        AnnoFieldTypes.put(annoType, types);
        if(!AnnoTypes.contains(annoType)){
            AnnoTypes.add(annoType);
        }
    }

    public static String resolveFieldKey(Class<? extends Annotation> annoType){
        return AnnoFieldName.get(annoType);
    }

    public static String resolveEnableKey(Class<? extends Annotation> annoType){
        return AnnoEnable.get(annoType);
    }
    public static Class[] resolveTypes(Class<? extends Annotation> annoType){
        return AnnoFieldTypes.get(annoType);
    }

    public static void foreach(Consumer<Class<? extends Annotation>> consumer){
        AnnoTypes.forEach(consumer);
    }

    public static boolean anyMatch(Predicate<Class<? extends Annotation>> consumer){
        return AnnoTypes.stream().anyMatch(consumer);
    }
}
