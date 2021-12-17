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

    public final static String ALIAS = "T0";
    public final static String ID_KEY_DEFAULT = "id";

    private Class entity;
    private String table;
    private List<TableJoin> joins;
    private List<TableOrderBy> orderBys;
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

    @Setter
    @Getter
    public static class EntityField {
        private String name;
        private String column;
        private String table;
        private Class<?> javaType;
        private String pattern;
    }

    @Getter
    @Setter
    public static class TableJoin {
        private Class entity;
        private String alias;
        private String onLeft;
        private String onRight;
        private JoinType joinType;
    }

    @Getter
    @Setter
    public static class TableOrderBy {
        private String field;
        private SortType sortType;
    }
}
