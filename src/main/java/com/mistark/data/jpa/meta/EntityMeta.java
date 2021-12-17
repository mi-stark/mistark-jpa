package com.mistark.data.jpa.meta;

import lombok.Getter;
import lombok.Setter;
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

}
