package com.mistark.data.jpa.meta;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class EntityMeta {
    public final static String ALIAS = "T0";

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
}
