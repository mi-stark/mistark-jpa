package com.mistark.data.jpa.meta;


import com.mistark.data.jpa.annotation.SortType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.criteria.JoinType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class EntityMeta {

    private Class entity;
    private String table;
    private String tableAlias;
    private List<TableJoin> joins;
    private List<TableOrderBy> orderBys;
    private List<String> groupBys;
    private EntityField id;
    private EntityField createBy;
    private EntityField createDate;
    private EntityField updateBy;
    private EntityField updateDate;
    private EntityField softDel;
    private EntityField tenantId;
    private EntityField version;
    private Map<String, EntityField> fields;

    public Collection<EntityField> fields(){
        return fields!=null ? fields.values() : Collections.EMPTY_LIST;
    }

    public EntityField resolve(String field){
        return fields!=null ? fields.get(field) : null;
    }

    public boolean isSoftDel(){
        return softDel!=null;
    }

    public EntityMeta clone(){
        EntityMeta meta = new EntityMeta();
        meta.setEntity(entity);
        meta.setTable(table);
        meta.setTableAlias(tableAlias);
        meta.setJoins(joins);
        meta.setOrderBys(orderBys);
        meta.setGroupBys(groupBys);
        meta.setId(id);
        meta.setCreateBy(createBy);
        meta.setCreateDate(createDate);
        meta.setUpdateBy(updateBy);
        meta.setUpdateDate(updateDate);
        meta.setSoftDel(softDel);
        meta.setTenantId(tenantId);
        meta.setVersion(version);
        meta.setFields(fields);
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
