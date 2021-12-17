package com.mistark.data.jpa.helper;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;

import java.util.Date;

public class ConvertHelper {

    public static Object convert(Object value, final Class<?> targetType, String...patterns) {
        if(targetType.isAssignableFrom(Date.class)){
            DateConverter dateConverter = new DateConverter();
            dateConverter.setPatterns(patterns);
            value = dateConverter.convert(targetType, value);
        }else {
            value = ConvertUtils.convert(value, targetType);
        }
        return value;
    }
}
