package com.mistark.data.jpa.annotation;

import com.mistark.data.jpa.builder.JpaMethodParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindParser {
    Class<? extends JpaMethodParser> value();
}
