package com.mistark.data.jpa.meta;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EntityField {
    private String name;
    private String column;
    private String table;
    private Class<?> javaType;
    private String pattern;
}
