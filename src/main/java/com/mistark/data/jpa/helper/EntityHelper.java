package com.mistark.data.jpa.helper;

import com.mistark.data.jpa.annotation.*;
import com.mistark.data.jpa.meta.EntityField;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.meta.TableJoin;
import com.mistark.data.jpa.meta.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityHelper {

    private static Map<Integer, EntityMeta> KnownMetas = new ConcurrentHashMap<>();

    public static EntityMeta resolve(Class entity){
        if(entity == null) return null;
        return KnownMetas.computeIfAbsent(entity.hashCode(), k -> {
            EntityMeta meta = new EntityMeta();
            meta.setEntity(entity);
            Table table = AnnotationUtils.getAnnotation(entity, Table.class);
            if(table!=null && table.joins().length > 0){
                List<TableJoin> joins = Arrays.stream(table.joins()).map(join -> {
                    TableJoin tableJoin = new TableJoin();
                    EntityMeta entityMeta = join.entity().equals(entity) ? meta : resolve(join.entity());
                    tableJoin.setEntityMeta(entityMeta);
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
            KnownMetas.put(tableName.hashCode(), meta);
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
                Value<Boolean> skip = new Value<>(false);
                Arrays.stream(annoTypes).anyMatch(t -> {
                    Annotation anno = AnnotationUtils.getAnnotation(field, t);
                    if(anno!=null) {
                        skip.set(anno instanceof SoftDel);
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
                entityField.setName(field.getName());
                entityField.setColumn(columnName);
                entityField.setJavaType(field.getType());
                entityField.setTable(columnTable);
                if(skip.get()) return;
                fieldMap.put(entityField.getName(), entityField);
            });
            MetaObject metaObject = SystemMetaObject.forObject(meta);
            Arrays.stream(annoTypes).forEach(t -> metaObject.setValue(StringUtils.uncapitalize(t.getSimpleName()), annoFieldMap.get(t)));
            if(meta.getId() == null) meta.setId(fieldMap.get("id"));
            if(meta.getId()!=null && meta.getId()==meta.getSoftDel()){
                throw new BuilderException("primary key id and soft delete flag cannot be the same field");
            }
            return meta;
        });
    }

    public static EntityMeta fromMapper(Class mapper){
        if(mapper == null) return null;
        return KnownMetas.computeIfAbsent(mapper.hashCode(), k-> {
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
        try {
            Class mapper = Class.forName(name);
            return fromMapper(mapper);
        }catch (Throwable e){
            return null;
        }
    }

}