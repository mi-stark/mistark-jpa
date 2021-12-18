package com.mistark.data.jpa.annotation;

import javax.persistence.criteria.JoinType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Join {
    String table();
    String alias();
    String on();
    JoinType joinType() default JoinType.LEFT;
}
