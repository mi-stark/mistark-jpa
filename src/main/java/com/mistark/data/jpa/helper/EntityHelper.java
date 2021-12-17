package com.mistark.data.jpa.helper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mistark.data.jpa.annotation.*;
import com.mistark.data.jpa.meta.EntityField;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.meta.TableJoin;
import com.mistark.meta.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityHelper {

    private final static Map<Integer, EntityMeta> KnownMetas = new ConcurrentHashMap<>();
    private final static Map<Integer, EntityMeta> KnownTableNameMetas = new ConcurrentHashMap<>();
    private final static Map<Integer, EntityMeta> KnownMapperMetas = new ConcurrentHashMap<>();
    private final static Map<Integer, EntityMeta> KnownMapperNameMetas = new ConcurrentHashMap<>();
    private final static Map<Integer, EntityMeta> KnownMethodMetas = new ConcurrentHashMap<>();
    private final static Map<Integer, EntityMeta> KnownMethodNameMetas = new ConcurrentHashMap<>();

    public static boolean isEntity(Class entity){
        return entity!=null && AnnotationUtils.getAnnotation(entity, Entity.class)!=null;
    }

    public static boolean isEntity(Object obj){
        return obj!=null && isEntity(obj.getClass());
    }

    public static EntityMeta resolve(Class entity){
        if(!isEntity(entity)) return null;
        return KnownMetas.computeIfAbsent(entity.hashCode(), k -> {
            EntityMeta meta = new EntityMeta();
            meta.setEntity(entity);
            Table table = AnnotationUtils.getAnnotation(entity, Table.class);
            if(table!=null && table.joins().length > 0){
                List<TableJoin> joins = Arrays.stream(table.joins()).map(join -> {
                    TableJoin tableJoin = new TableJoin();
                    tableJoin.setEntity(join.entity());
                    tableJoin.setAlias(join.alias());
                    tableJoin.setOnLeft(join.onLeft());
                    tableJoin.setOnRight(join.onRight());
                    tableJoin.setJoinType(join.joinType());
                    return tableJoin;
                }).collect(Collectors.toList());
                meta.setJoins(joins);
            }
            String tableName = table!=null ? table.name().trim() : null;
            if(StringUtils.isEmpty(tableName)){
                tableName = StringHelper.toUnderline(entity.getSimpleName()).toLowerCase();
            }
            KnownTableNameMetas.put(tableName.hashCode(), meta);
            meta.setTable(tableName);
            Map<String, EntityField> fieldMap = new ConcurrentHashMap<>();
            meta.setFields(fieldMap);
            Map<Class, EntityField> annoFieldMap = new ConcurrentHashMap<>();
            Class[] annoTypes = new Class[]{
                    Id.class,
                    TenantId.class,
                    CreateBy.class,
                    CreateDate.class,
                    UpdateBy.class,
                    UpdateDate.class,
                    SoftDel.class,
                    Version.class
            };
            FieldUtils.getAllFieldsList(entity).forEach(field -> {
                Column column = AnnotationUtils.getAnnotation(field, Column.class);
                if(column == null) return;
                EntityField entityField = new EntityField();
                Arrays.stream(annoTypes).anyMatch(t -> {
                    Annotation anno = AnnotationUtils.getAnnotation(field, t);
                    if(anno!=null) {
                        annoFieldMap.put(t, entityField);
                        return true;
                    }
                    return false;
                });
                String columnName = column.name().trim();
                if(StringUtils.isEmpty(columnName)){
                    columnName = StringHelper.toUnderline(field.getName()).toLowerCase();
                }
                String columnTable = column.table().trim();
                if(StringUtils.isEmpty(columnTable)){
                    columnTable = EntityMeta.ALIAS;
                }
                JsonFormat jsonFormat = AnnotationUtils.getAnnotation(field, JsonFormat.class);
                String pattern = jsonFormat==null ? null : jsonFormat.pattern();
                entityField.setName(field.getName());
                entityField.setColumn(columnName);
                entityField.setJavaType(field.getType());
                entityField.setTable(columnTable);
                entityField.setPattern(pattern);
                fieldMap.put(entityField.getName(), entityField);
            });
            MetaObject metaObject = SystemMetaObject.forObject(meta);
            Arrays.stream(annoTypes).forEach(t -> metaObject.setValue(StringUtils.uncapitalize(t.getSimpleName()), annoFieldMap.get(t)));
            if(meta.getId()!=null && meta.getId()==meta.getSoftDel()){
                throw new BuilderException("primary key id and soft delete flag cannot be the same field");
            }
            if(meta.getId()==null) meta.setId(meta.resolve(EntityMeta.ID_KEY_DEFAULT));
            return meta;
        });
    }

    public static EntityMeta resolve(String table){
        return KnownTableNameMetas.get(table.toLowerCase().hashCode());
    }

    public static EntityMeta fromMapper(Class mapper){
        return KnownMapperMetas.computeIfAbsent(mapper.hashCode(), k-> {
            BindEntity bindEntity = AnnotationUtils.getAnnotation(mapper, BindEntity.class);
            Value<Class> entity = new Value<>();
            if(bindEntity!=null) entity.set(bindEntity.value());
            else {
                Arrays.stream(mapper.getGenericInterfaces()).anyMatch(t -> {
                    if(t instanceof ParameterizedType){
                        Type[] types = ((ParameterizedType)t).getActualTypeArguments();
                        if(types.length > 0 && !(types[0] instanceof TypeVariable || types[0] instanceof WildcardType)) {
                            entity.set((Class) types[0]);
                        }
                        return true;
                    }
                    return false;
                });
            }
            return resolve(entity.get());
        });
    }

    public static EntityMeta fromMapper(String name){
        return KnownMapperNameMetas.computeIfAbsent(name.hashCode(), k -> {
            try {
                Class mapper = Class.forName(name);
                return fromMapper(mapper);
            }catch (Throwable e){
                return null;
            }
        });
    }

    public static EntityMeta fromMethod(Class type, Method method){
        return KnownMethodMetas.computeIfAbsent(method.hashCode(), k -> {
            BindEntity bindEntity = AnnotationUtils.getAnnotation(method, BindEntity.class);
            return bindEntity != null ? EntityHelper.resolve(bindEntity.value()) : EntityHelper.fromMapper(type);
        });
    }

    public static EntityMeta fromMethod(String name){
        return KnownMethodNameMetas.computeIfAbsent(name.hashCode(), k->{
            Pattern pattern = Pattern.compile("^(.+)\\.(.+)$");
            Matcher matcher = pattern.matcher(name);
            if(!matcher.matches()) return null;
            try {
                Class type = Class.forName(matcher.group(1));
                Value<Method> method = new Value<>();
                Arrays.stream(type.getMethods()).anyMatch(m -> {
                    boolean match = !m.isDefault() && m.getName().equals(matcher.group(2));
                    method.set(match ? m : null);
                    return match;
                });
                return method.get()!=null ? fromMethod(type, method.get()) : null;
            } catch (ClassNotFoundException e) {
                return null;
            }
        });
    }

    public static EntityMeta fromStatement(MappedStatement ms){
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        Class entity = null;
        switch (sqlCommandType){
            case SELECT:
                if(ms.getResultMaps().size() > 0){
                    entity = ms.getResultMaps().get(0).getType();
                }
                break;
            case INSERT:
            case UPDATE:
            case DELETE:
                entity = ms.getParameterMap().getType();
                break;
            default: break;
        }
        return isEntity(entity) ? resolve(entity) : fromMethod(ms.getId());
    }
}