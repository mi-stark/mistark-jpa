package com.mistark.data.jpa.meta;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;

@Setter
@Getter
public class EntityField {
    private Field field;
    private String name;
    private String column;
    private String table;
    private Class<?> javaType;
}
