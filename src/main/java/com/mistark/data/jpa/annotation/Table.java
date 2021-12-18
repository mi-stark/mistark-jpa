package com.mistark.data.jpa.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    String name() default "";
    String alias() default ALIAS_DEFAULT;
    Join[] joins() default {};
    OrderBy[] orderBys() default {};
    String[] groupBys() default {};

    String ALIAS_DEFAULT = "T0";
    String ID_DEFAULT = "id";
}
