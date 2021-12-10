package com.mistark.data.jpa.meta;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.criteria.JoinType;

@Getter
@Setter
public class TableJoin {
    private EntityMeta entityMeta;
    private String alias;
    private String onLeft;
    private String onRight;
    private JoinType joinType;
}
