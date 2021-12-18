package com.mistark.data.jpa.helper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mistark.data.jpa.annotation.*;
import com.mistark.data.jpa.meta.EntityMeta;
import com.mistark.data.jpa.meta.EntityMeta.*;
import com.mistark.meta.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityHelper {

    private final static Map<Integer, EntityMeta> KnownMetas = new ConcurrentHashMap<>();
    private final static Map<Integer, EntityMeta> KnownTableNameMetas = new ConcurrentHashMap<>();
    private final static Map<Integer, EntityMeta> KnownMethodNameMetas = new ConcurrentHashMap<>();

    public static boolean isEntity(Class entity){
        return entity!=null && AnnotationUtils.getAnnotation(entity, Entity.class)!=null;
    }

    public static EntityMeta resolve(Class entity){
        if(!isEntity(entity)) return null;
        return KnownMetas.computeIfAbsent(entity.hashCode(), k -> {
            EntityMeta meta = new EntityMeta();
            meta.setEntity(entity);
            Map<String, TableOrderBy> orderByMap = new HashMap<>();
            Set<String> groupBys = new HashSet<>();
            Table annoTable = AnnotationUtils.getAnnotation(entity, Table.class);
            String table = null;
            String tableAlias = null;
            if(annoTable!=null){
                table = annoTable.name().trim();
                tableAlias = annoTable.alias().trim();
                if(annoTable.joins().length > 0){
                    List<TableJoin> joins = Arrays.stream(annoTable.joins()).map(join -> {
                        TableJoin tableJoin = new TableJoin();
                        tableJoin.setTable(join.table().trim());
                        tableJoin.setAlias(join.alias().trim());
                        tableJoin.setOn(join.on().trim());
                        tableJoin.setJoinType(join.joinType());
                        String scahma = "[A-Za-z]\\w{0,3}\\.[A-Za-z]\\w{0,29}";
                        String pattern = String.format("^%s *= *%s$", scahma, scahma);
                        Matcher matcher = Pattern.compile(pattern).matcher(tableJoin.getOn());
                        if(!matcher.matches()){
                            throw new BuilderException("Invalid value at @Join.on：" + tableJoin.getOn());
                        }
                        return tableJoin;
                    }).collect(Collectors.toList());
                    meta.setJoins(joins);
                }
                if(annoTable.groupBys().length > 0){
                    Arrays.stream(annoTable.groupBys()).forEach(field -> {
                        if(StringUtils.isEmpty(field.trim())) return;
                        groupBys.add(field.trim());
                    });
                }
                if(annoTable.groupBys().length > 0){
                    Arrays.stream(annoTable.orderBys()).forEach(orderBy -> {
                        TableOrderBy tableOrderBy = new TableOrderBy();
                        tableOrderBy.setField(orderBy.name().trim());
                        tableOrderBy.setSortType(orderBy.sortType());
                        orderByMap.put(tableOrderBy.getField(), tableOrderBy);
                    });
                }
            }
            table = StringUtils.isEmpty(table)
                    ? StringHelper.toUnderline(entity.getSimpleName()).toLowerCase()
                    : table;
            tableAlias = StringUtils.isEmpty(tableAlias) ? Table.ALIAS_DEFAULT : tableAlias;
            meta.setTable(table);
            meta.setTableAlias(tableAlias);
            Map<String, EntityField> fieldMap = new HashMap<>();
            meta.setFields(fieldMap);
            Map<Class, EntityField> annoFieldMap = new HashMap<>();
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
            FieldUtils.getAllFieldsList(entity).forEach(fl -> {
                Column annoColumn = AnnotationUtils.getAnnotation(fl, Column.class);
                if(annoColumn == null) return;
                EntityField field = new EntityField();
                Arrays.stream(annoTypes).anyMatch(type -> {
                    Annotation anno = AnnotationUtils.getAnnotation(fl, type);
                    if(anno!=null) {
                        if(type.equals(SoftDel.class) && !((SoftDel)anno).enable()){
                            return false;
                        }
                        annoFieldMap.put(type, field);
                        return true;
                    }
                    return false;
                });
                OrderBy orderBy = AnnotationUtils.getAnnotation(fl, OrderBy.class);
                if(orderBy!=null && !orderByMap.containsKey(fl.getName())){
                    TableOrderBy tableOrderBy = new TableOrderBy();
                    tableOrderBy.setSortType(orderBy.sortType());
                    tableOrderBy.setField(fl.getName());
                    orderByMap.put(tableOrderBy.getField(), tableOrderBy);
                }
                GroupBy groupBy = AnnotationUtils.getAnnotation(fl, GroupBy.class);
                if(groupBy!=null && !groupBys.contains(fl.getName())){
                    groupBys.add(fl.getName());
                }
                JsonFormat jsonFormat = AnnotationUtils.getAnnotation(fl, JsonFormat.class);
                String pattern = jsonFormat==null ? null : jsonFormat.pattern();
                String column = annoColumn.name().trim();
                column = !StringUtils.isEmpty(column) ? column : StringHelper.toUnderline(fl.getName()).toLowerCase();
                String columnTableAlias = annoColumn.tableAlias().trim();
                columnTableAlias = StringUtils.isEmpty(columnTableAlias) ? meta.getTableAlias() : columnTableAlias;

                field.setName(fl.getName());
                field.setColumn(column);
                field.setTableAlias(columnTableAlias);
                field.setJavaType(fl.getType());
                field.setPattern(pattern);
                fieldMap.put(field.getName(), field);
            });
            MetaObject metaObject = SystemMetaObject.forObject(meta);
            Arrays.stream(annoTypes).forEach(anno -> {
                String key = StringUtils.uncapitalize(anno.getSimpleName());
                metaObject.setValue(key, annoFieldMap.get(anno));
            });
            if(orderByMap.size()>0){
                meta.setOrderBys(orderByMap.values().stream().collect(Collectors.toList()));
            }

            if(groupBys.size()>0){
                meta.setGroupBys(groupBys.stream().collect(Collectors.toList()));
            }

            if(meta.getId()==null) {
                meta.setId(meta.resolve(Table.ID_DEFAULT));
            }
            if(meta.getId()!=null){
                if(meta.getId()==meta.getSoftDel()){
                    throw new BuilderException("ID primary key cannot be used as soft delete");
                }
                if(!Number.class.isAssignableFrom(meta.getId().getJavaType())
                        || String.class.isAssignableFrom(meta.getId().getJavaType())){
                    throw new BuilderException("Wrong type for ID primary key："+meta.getId().getJavaType().getName());
                }
            }else {
                throw new BuilderException("ID primary key is required");
            }

            SoftDel annoSoftDel = AnnotationUtils.getAnnotation(entity, SoftDel.class);
            SoftDelHelper.checkSoftDel(annoSoftDel, meta);

            if(meta.getVersion()!=null && !Number.class.isAssignableFrom(meta.getVersion().getJavaType())){
                throw new BuilderException("Wrong type for version：" + meta.getVersion().getJavaType().getName());
            }

            KnownTableNameMetas.put(meta.getTable().hashCode(), meta);
            return meta;
        });
    }

    public static EntityMeta resolve(String table){
        return KnownTableNameMetas.get(table.toLowerCase().hashCode());
    }

    public static EntityMeta fromMapper(Class mapper){
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
    }

    public static EntityMeta fromMethod(Class type, Method method){
        BindEntity bindEntity = AnnotationUtils.getAnnotation(method, BindEntity.class);
        EntityMeta meta = bindEntity != null ? EntityHelper.resolve(bindEntity.value()) : EntityHelper.fromMapper(type);
        SoftDel annoSoftDel = AnnotationUtils.getAnnotation(method, SoftDel.class);
        if(meta!=null && annoSoftDel!=null) {
            meta = meta.clone();
            SoftDelHelper.checkSoftDel(annoSoftDel, meta);
        }
        String key = type.getName() + "." + method.getName();
        KnownMethodNameMetas.put(key.hashCode(), meta);
        return meta;
    }

    public static EntityMeta fromMethod(String name){
        return KnownMethodNameMetas.get(name.hashCode());
    }

    public static EntityMeta fromStatement(MappedStatement ms){
        EntityMeta meta = fromMethod(ms.getId());
        if(meta == null){
            Class entity = null;
            switch (ms.getSqlCommandType()){
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
            meta = isEntity(entity) ? resolve(entity) : null;
        }
        return meta;
    }
}