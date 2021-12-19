package com.mistark.data.jpa.meta;


import com.mistark.data.jpa.annotation.Id;
import com.mistark.data.jpa.annotation.SoftDel;
import com.mistark.data.jpa.annotation.SortType;
import com.mistark.data.jpa.annotation.Table;
import com.mistark.data.jpa.helper.AnnoFieldHelper;
import com.mistark.meta.Value;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.BuilderException;
import org.springframework.core.annotation.AnnotationUtils;

import javax.persistence.criteria.JoinType;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class EntityMeta {

    @Getter
    @Setter
    private Class entity;

    @Getter
    @Setter
    private String table;

    @Getter
    @Setter
    private String tableAlias;

    @Getter
    @Setter
    private List<TableJoin> joins;

    @Getter
    @Setter
    private List<TableOrderBy> orderBys;

    @Getter
    @Setter
    private List<String> groupBys;
    private Map<String, EntityField> fields = new ConcurrentHashMap<>();
    private Map<String, EntityField> annoFields = new ConcurrentHashMap<>();

    public Collection<EntityField> fields(){
        return fields.values();
    }

    public void addField(EntityField field){
        fields.put(field.getName(), field);
    }

    public EntityField resolve(String field){
        return fields.get(field);
    }
    
    public Class resolveFieldType(String field){
        return resolve(field).getJavaType();
    }

    public Collection<EntityField> annoFields(){
        return annoFields.values();
    }

    public void addAnnoField(String annoFieldName, EntityField field){
        annoFields.put(annoFieldName, field);
    }

    public void addAnnoField(Class<? extends Annotation> annoType, EntityField field){
        addAnnoField(annoType.getSimpleName(), field);
    }
    
    public boolean hasAnnoField(String annoName){
        return annoFields.containsKey(annoName);
    }
    
    public boolean hasAnnoField(Class annoType){
        return hasAnnoField(annoType.getSimpleName());
    }

    public void removeAnnoField(String annoName){
        annoFields.remove(annoName);
    }

    public void removeAnnoField(Class annoType){
        removeAnnoField(annoType.getSimpleName());
    }

    public EntityField annoField(String field){
        return annoFields.get(field);
    }

    public EntityField annoField(Class<? extends Annotation> annoType){
        return annoField(annoType.getSimpleName());
    }

    public String annoFieldName(String field){
        return annoField(field).getName();
    }

    public String annoFieldName(Class<? extends Annotation> annoType){
        return annoFieldName(annoType.getSimpleName());
    }

    public String annoFieldColumn(String field){
        return annoField(field).getColumn();
    }

    public String annoFieldColumn(Class<? extends Annotation> annoType){
        return annoFieldColumn(annoType.getSimpleName());
    }

    public Class annoFieldType(String field){
        return annoField(field).getJavaType();
    }

    public Class annoFieldType(Class<? extends Annotation> annoType){
        return annoFieldType(annoType.getSimpleName());
    }
    
    public boolean isSoftDel(){
        return hasAnnoField(SoftDel.class);
    }

    public void validate(){
        validate(annoType -> AnnotationUtils.getAnnotation(entity, annoType), null);
    }

    public void validate(Function<Class<? extends Annotation>, Annotation> getAnno, Value<EntityMeta> cloned){
        Value<EntityMeta> metaValue = new Value<>(this);
        AnnoFieldHelper.foreach(annoType -> {
            Annotation anno = getAnno.apply(annoType);
            if(anno==null) return;
            if(cloned!=null){
                metaValue.set(metaValue.get().clone());
            }
            EntityMeta meta = metaValue.get();
            AnnoField annoField = new AnnoField(anno, annoType);
            if(annoField.isEnabled()) {
                String fieldName = annoField.getField();
                fieldName = fieldName!=null ? fieldName.trim() : fieldName;
                EntityField field;
                if(StringUtils.isEmpty(fieldName)){
                    field = meta.annoField(annoType);
                }else {
                    field = meta.resolve(fieldName);
                }
                annoField.setEntityField(field);
                annoField.checkType();
                meta.addAnnoField(annoType, field);
            }else {
                meta.removeAnnoField(annoType);
            }
        });
        if(cloned!=null && metaValue.get().equals(this)){
            cloned.set(metaValue.get());
        }
        EntityMeta meta = metaValue.get();
        if(!meta.hasAnnoField(Id.class)){
            meta.addAnnoField(Id.class, resolve(Table.ID_DEFAULT));
        }
        if(meta.hasAnnoField(Id.class)){
            if(meta.annoField(Id.class) == meta.annoField(SoftDel.class)){
                throw new BuilderException("ID primary key cannot be used as soft delete");
            }
        }else {
            throw new BuilderException("ID primary key is required");
        }
    }

    public EntityMeta clone(){
        EntityMeta meta = new EntityMeta();
        meta.setEntity(entity);
        meta.setTable(table);
        meta.setTableAlias(tableAlias);
        meta.setJoins(joins);
        meta.setOrderBys(orderBys);
        meta.setGroupBys(groupBys);
        fields().stream().forEach(f-> meta.addField(f));
        annoFields.entrySet().forEach(entry -> meta.addAnnoField(entry.getKey(), entry.getValue()));
        return meta;
    }


    @Setter
    @Getter
    public static class EntityField {
        private String name;
        private String column;
        private String tableAlias;
        private Class<?> javaType;
        private String pattern;
    }

    @Getter
    @Setter
    public static class TableJoin {
        private String table;
        private String alias;
        private String on;
        private JoinType joinType;
    }

    @Getter
    @Setter
    public static class TableOrderBy {
        private String field;
        private SortType sortType;
    }

}
